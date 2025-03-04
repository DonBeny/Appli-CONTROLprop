package org.orgaprop.test7.metrics.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.HashMap;

public class MetricsMonitor implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsMonitor.class);

	private final Map<String, MetricTracker> metricTrackers;
	private final ScheduledExecutorService scheduler;
	private final AlertSystem alertSystem;
	private final AtomicBoolean isRunning;
	private final MetricsStatistics statistics;
	private final MetricsConfig config;
	private final ConfigModule monitoringConfig;

	public MetricsMonitor(AlertSystem alertSystem) {
		this.config = MetricsConfig.getInstance();
		this.monitoringConfig = config.getModule("monitoring");

		if (monitoringConfig == null) {
			logger.error("Configuration 'monitoring' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> schedulerConfig = (Map<String, Object>) monitoringConfig.getProperty("scheduler");
		int threadPoolSize = (int) schedulerConfig.get("threadPoolSize");

		this.metricTrackers = new ConcurrentHashMap<>();
		this.scheduler = Executors.newScheduledThreadPool(threadPoolSize);
		this.alertSystem = alertSystem;
		this.isRunning = new AtomicBoolean(true);
		this.statistics = new MetricsStatistics();

		startMonitoring();
	}

	private void startMonitoring() {
		Map<String, Object> schedulerConfig = (Map<String, Object>) monitoringConfig.getProperty("scheduler");
		long checkInterval = (long) schedulerConfig.get("checkInterval");

		scheduler.scheduleAtFixedRate(
				this::checkMetrics,
				checkInterval,
				checkInterval,
				TimeUnit.MILLISECONDS);
	}

	public void recordMetric(String name, double value) {
		if (!isRunning.get()) {
			logger.warn("Tentative d'enregistrement sur un moniteur arrêté");
			return;
		}

		metricTrackers.computeIfAbsent(name, k -> new MetricTracker(name))
				.recordValue(value);

		if (anomalyDetector.isAnomaly(name, value)) {
			handleAnomaly(name, value);
		}

		statistics.updateStats(name, value);
	}

	private void checkMetrics() {
		if (!isRunning.get())
			return;

		Map<String, Object> performanceConfig = (Map<String, Object>) monitoringConfig.getProperty("performance");
		double anomalyThreshold = (double) performanceConfig.get("anomalyThreshold");
		int minSampleSize = (int) performanceConfig.get("minSampleSize");

		try {
			for (MetricTracker tracker : metricTrackers.values()) {
				tracker.analyze(anomalyThreshold, minSampleSize);
			}
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification des métriques", e);
		}
	}

	private void handleAnomaly(String metricName, double value) {
		Map<String, Object> alertData = new HashMap<>();
		alertData.put("metric", metricName);
		alertData.put("value", value);
		alertData.put("threshold", ANOMALY_THRESHOLD);
		alertData.put("stats", statistics.getStats(metricName));

		alertSystem.raiseAlert("METRIC_ANOMALY", alertData);
		logger.warn("Anomalie détectée pour {}: {}", metricName, value);
	}

	private class MetricTracker {
		private final String name;
		private final CircularFifoQueue<Double> history;
		private final AtomicReference<Statistics> currentStats;

		MetricTracker(String name) {
			this.name = name;
			this.history = new CircularFifoQueue<>(MAX_METRIC_HISTORY);
			this.currentStats = new AtomicReference<>(new Statistics());
		}

		void recordValue(double value) {
			history.add(value);
			updateStatistics(value);
		}

		void analyze(double anomalyThreshold, int minSampleSize) {
			Statistics stats = currentStats.get();
			if (stats.isOutlier(anomalyThreshold, minSampleSize)) {
				reportOutlier(stats);
			}
		}

		private void updateStatistics(double value) {
			Statistics current = currentStats.get();
			current.update(value);
		}

		private void reportOutlier(Statistics stats) {
			Map<String, Object> alertData = new HashMap<>();
			alertData.put("metric", name);
			alertData.put("stats", stats.toMap());

			alertSystem.raiseAlert("METRIC_OUTLIER", alertData);
		}
	}

	private static class Statistics {
		private double mean;
		private double standardDeviation;
		private long count;

		void update(double value) {
			double delta = value - mean;
			count++;
			mean += delta / count;
			standardDeviation = Math.sqrt((standardDeviation * standardDeviation * (count - 1) +
					delta * (value - mean)) / count);
		}

		boolean isOutlier(double anomalyThreshold, int minSampleSize) {
			return standardDeviation > anomalyThreshold && count > minSampleSize;
		}

		Map<String, Object> toMap() {
			return Map.of(
					"mean", mean,
					"standardDeviation", standardDeviation,
					"count", count);
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				Map<String, Object> schedulerConfig = (Map<String, Object>) monitoringConfig.getProperty("scheduler");
				long gracePeriod = (long) schedulerConfig.get("gracePeriod");

				if (!scheduler.awaitTermination(gracePeriod, TimeUnit.MILLISECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
		}
	}
}
