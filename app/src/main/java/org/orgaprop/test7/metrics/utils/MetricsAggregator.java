package org.orgaprop.test7.metrics.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MetricsAggregator implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsAggregator.class);

	private static final long AGGREGATION_INTERVAL = 60_000; // 1 minute
	private static final int MAX_AGGREGATION_WINDOWS = 1440; // 24 heures
	private static final int BATCH_SIZE = 1000;

	private final Map<String, AggregationWindow> timeWindows;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final BlockingQueue<MetricPoint> inputQueue;
	private final AlertManager alertManager;
	private final AggregationMetrics metrics;

	public MetricsAggregator(AlertManager alertManager) {
		this.timeWindows = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.inputQueue = new LinkedBlockingQueue<>(BATCH_SIZE);
		this.alertManager = alertManager;
		this.metrics = new AggregationMetrics();

		startAggregation();
	}

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
			metrics.recordSubmission();
		} catch (Exception e) {
			handleMetricError(e, name);
		}
	}

	private void startAggregation() {
		scheduler.scheduleAtFixedRate(
				this::processMetricsBatch,
				AGGREGATION_INTERVAL,
				AGGREGATION_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void processMetricsBatch() {
		List<MetricPoint> batch = new ArrayList<>(BATCH_SIZE);
		inputQueue.drainTo(batch);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processBatch(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	private void processBatch(List<MetricPoint> batch) {
		try {
			Map<String, List<MetricPoint>> groupedMetrics = batch.stream()
					.collect(groupingBy(MetricPoint::getName));

			groupedMetrics.forEach(this::aggregateMetricGroup);
			cleanupOldWindows();
		} catch (Exception e) {
			handleBatchError(e, batch.size());
		}
	}

	private void aggregateMetricGroup(String metricName, List<MetricPoint> points) {
		AggregationWindow window = timeWindows.computeIfAbsent(
				metricName,
				k -> new AggregationWindow());

		window.addPoints(points);
		checkThresholds(metricName, window);
	}

	private void checkThresholds(String metricName, AggregationWindow window) {
		AggregationStats stats = window.getStats();
		if (stats.isAnomalous()) {
			alertManager.raiseAlert("METRIC_ANOMALY", Map.of(
					"metric", metricName,
					"stats", stats.toMap(),
					"threshold", stats.getThreshold()));
		}
	}

	public Map<String, Object> getAggregatedStats(String metricName) {
		AggregationWindow window = timeWindows.get(metricName);
		return window != null ? window.getStats().toMap() : Map.of();
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

	private static class AggregationWindow {
		private final AtomicLong count = new AtomicLong(0);
		private final AtomicDouble sum = new AtomicDouble(0);
		private final AtomicDouble sumSquares = new AtomicDouble(0);
		private final AtomicDouble min = new AtomicDouble(Double.MAX_VALUE);
		private final AtomicDouble max = new AtomicDouble(Double.MIN_VALUE);
		private final long creationTime = System.currentTimeMillis();

		void addPoints(List<MetricPoint> points) {
			points.forEach(point -> {
				count.incrementAndGet();
				double value = point.getValue();
				sum.addAndGet(value);
				sumSquares.addAndGet(value * value);
				updateMin(value);
				updateMax(value);
			});
		}

		private void updateMin(double value) {
			min.updateAndGet(current -> Math.min(current, value));
		}

		private void updateMax(double value) {
			max.updateAndGet(current -> Math.max(current, value));
		}

		AggregationStats getStats() {
			return new AggregationStats(
					count.get(),
					sum.get(),
					sumSquares.get(),
					min.get(),
					max.get(),
					creationTime);
		}
	}
}
