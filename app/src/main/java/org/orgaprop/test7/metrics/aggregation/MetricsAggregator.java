package org.orgaprop.test7.metrics.aggregation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import org.orgaprop.test7.metrics.management.AlertManager;

/**
 * Gestionnaire d'agrégation des métriques.
 * Cette classe est responsable de l'agrégation des métriques en temps réel.
 */
public class MetricsAggregator implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsAggregator.class);

	private static final long AGGREGATION_INTERVAL = 60_000; // 1 minute
	private static final int MAX_AGGREGATION_WINDOWS = 1440; // 24 heures
	private static final int BATCH_SIZE = 1000;

	private final Map<String, AggregationWindow> timeWindows;
	private final Map<String, Map<String, AggregationStats>> dimensionalStats;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final BlockingQueue<MetricPoint> inputQueue;
	private final AlertManager alertManager;
	private final AggregatorMetrics metrics;

	/**
	 * Constructeur de MetricsAggregator.
	 *
	 * @param alertManager le gestionnaire d'alertes
	 */
	public MetricsAggregator(AlertManager alertManager) {
		this.timeWindows = new ConcurrentHashMap<>();
		this.dimensionalStats = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.inputQueue = new LinkedBlockingQueue<>(BATCH_SIZE);
		this.alertManager = alertManager;
		this.metrics = new AggregatorMetrics();

		startAggregation();
	}

	/**
	 * Ajoute une métrique à l'agrégateur.
	 *
	 * @param name  le nom de la métrique
	 * @param value la valeur de la métrique
	 * @param tags  les tags associés à la métrique
	 */
	public void addMetric(String name, double value, Map<String, Object> tags) {
		if (!isRunning.get()) {
			logger.warn("Tentative d'ajout de métrique alors que l'agrégateur est arrêté");
			return;
		}

		try {
			MetricPoint point = new MetricPoint(name, value, tags);
			if (!inputQueue.offer(point)) {
				handleQueueFull(point);
				return;
			}
			metrics.recordSubmission(name);
		} catch (Exception e) {
			handleMetricError(e, name);
		}
	}

	/**
	 * Démarre le processus d'agrégation.
	 */
	private void startAggregation() {
		scheduler.scheduleAtFixedRate(
				this::processMetricsBatch,
				AGGREGATION_INTERVAL,
				AGGREGATION_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * Traite un lot de métriques.
	 */
	private void processMetricsBatch() {
		List<MetricPoint> batch = new ArrayList<>(BATCH_SIZE);
		inputQueue.drainTo(batch);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processBatch(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	/**
	 * Traite un lot de métriques.
	 *
	 * @param batch le lot de métriques à traiter
	 */
	private void processBatch(List<MetricPoint> batch) {
		try {
			Map<String, List<MetricPoint>> groupedMetrics = batch.stream()
					.collect(groupingBy(MetricPoint::getName));

			groupedMetrics.forEach(this::aggregateMetricGroup);
			aggregateDimensionalMetrics(batch);
			cleanupOldWindows();
		} catch (Exception e) {
			handleBatchError(e, batch.size());
		}
	}

	/**
	 * Agrège un groupe de métriques.
	 *
	 * @param metricName le nom de la métrique
	 * @param points     les points de métriques à agréger
	 */
	private void aggregateMetricGroup(String metricName, List<MetricPoint> points) {
		AggregationWindow window = timeWindows.computeIfAbsent(
				metricName,
				k -> new AggregationWindow());

		window.addPoints(points);
		checkThresholds(metricName, window);
	}

	/**
	 * Agrège les métriques dimensionnelles.
	 *
	 * @param batch le lot de métriques à agréger
	 */
	private void aggregateDimensionalMetrics(List<MetricPoint> batch) {
		batch.forEach(point -> {
			point.getTags().forEach((dimension, value) -> {
				String key = point.getName() + "." + dimension;
				Map<String, AggregationStats> dimensionStats = this.dimensionalStats
						.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

				String valueKey = String.valueOf(value);
				AggregationStats stats = dimensionStats
						.computeIfAbsent(valueKey, k -> new AggregationStats());

				stats.addValue(point.getValue());
			});
		});
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
			cleanup();
		}
	}

	/**
	 * Point de métrique.
	 */
	private static class MetricPoint {
		private final String name;
		private final double value;
		private final Map<String, Object> tags;
		private final long timestamp;

		/**
		 * Constructeur de MetricPoint.
		 *
		 * @param name  le nom de la métrique
		 * @param value la valeur de la métrique
		 * @param tags  les tags associés à la métrique
		 */
		MetricPoint(String name, double value, Map<String, Object> tags) {
			this.name = name;
			this.value = value;
			this.tags = new HashMap<>(tags);
			this.timestamp = System.currentTimeMillis();
		}

		/**
		 * Obtient le nom de la métrique.
		 *
		 * @return le nom de la métrique
		 */
		String getName() {
			return name;
		}

		/**
		 * Obtient la valeur de la métrique.
		 *
		 * @return la valeur de la métrique
		 */
		double getValue() {
			return value;
		}

		/**
		 * Obtient les tags associés à la métrique.
		 *
		 * @return les tags associés à la métrique
		 */
		Map<String, Object> getTags() {
			return tags;
		}

		/**
		 * Obtient le timestamp de la métrique.
		 *
		 * @return le timestamp de la métrique
		 */
		long getTimestamp() {
			return timestamp;
		}
	}

	/**
	 * Métriques de l'agrégateur.
	 */
	private static class AggregatorMetrics {
		private final Map<String, AtomicLong> submissionsByMetric = new ConcurrentHashMap<>();
		private final AtomicLong totalProcessed = new AtomicLong(0);
		private final AtomicLong batchesProcessed = new AtomicLong(0);
		private final AtomicLong totalProcessingTime = new AtomicLong(0);

		/**
		 * Enregistre une soumission de métrique.
		 *
		 * @param metricName le nom de la métrique
		 */
		void recordSubmission(String metricName) {
			submissionsByMetric.computeIfAbsent(metricName, k -> new AtomicLong())
					.incrementAndGet();
		}

		/**
		 * Enregistre le traitement d'un lot de métriques.
		 *
		 * @param size     la taille du lot
		 * @param duration la durée de traitement
		 */
		void recordBatchProcessing(int size, long duration) {
			totalProcessed.addAndGet(size);
			batchesProcessed.incrementAndGet();
			totalProcessingTime.addAndGet(duration);
		}

		/**
		 * Obtient les statistiques de l'agrégateur.
		 *
		 * @return les statistiques de l'agrégateur
		 */
		Map<String, Object> getStats() {
			return Map.of(
					"submissions", new HashMap<>(submissionsByMetric),
					"totalProcessed", totalProcessed.get(),
					"batchesProcessed", batchesProcessed.get(),
					"averageProcessingTime", getAverageProcessingTime());
		}

		/**
		 * Obtient le temps de traitement moyen.
		 *
		 * @return le temps de traitement moyen
		 */
		private double getAverageProcessingTime() {
			long batches = batchesProcessed.get();
			return batches > 0 ? (double) totalProcessingTime.get() / batches : 0;
		}
	}
}