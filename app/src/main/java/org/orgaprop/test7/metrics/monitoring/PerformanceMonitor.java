package org.orgaprop.test7.metrics.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.NavigableMap;

public class PerformanceMonitor implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);

	private static final long CHECK_INTERVAL = 10_000; // 10 secondes
	private static final int HISTORY_SIZE = 1000;
	private static final double THRESHOLD_MULTIPLIER = 2.0;

	private final ScheduledExecutorService scheduler;
	private final NavigableMap<Long, PerformanceMetric> metricsHistory;
	private final Map<String, OperationStats> operationStats;
	private final AlertSystem alertSystem;
	private final AtomicBoolean isRunning;
	private final RollingAverage rollingAverage;
	private final PerformanceThreshold threshold;

	public PerformanceMonitor(AlertSystem alertSystem) {
		this.scheduler = createScheduler();
		this.metricsHistory = new ConcurrentSkipListMap<>();
		this.operationStats = new ConcurrentHashMap<>();
		this.alertSystem = alertSystem;
		this.isRunning = new AtomicBoolean(true);
		this.rollingAverage = new RollingAverage();
		this.threshold = new PerformanceThreshold();

		startMonitoring();
	}

	public void recordOperation(String operation, long duration) {
		if (!isRunning.get())
			return;

		try {
			OperationStats stats = operationStats.computeIfAbsent(operation,
					k -> new OperationStats(operation));
			stats.recordOperation(duration);

			if (duration > threshold.getThreshold(operation)) {
				handleSlowOperation(operation, duration, stats);
			}

			updateHistory(operation, duration);
		} catch (Exception e) {
			logger.error("Erreur lors de l'enregistrement de l'opération", e);
		}
	}

	private void startMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::analyzePerformance,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void analyzePerformance() {
		try {
			Map<String, Object> analysis = new HashMap<>();
			operationStats.forEach((op, stats) -> {
				analysis.put(op, stats.getStats());
				detectAnomalies(op, stats);
			});

			cleanupHistory();
			updateThresholds();
		} catch (Exception e) {
			logger.error("Erreur lors de l'analyse des performances", e);
		}
	}

	private void handleSlowOperation(String operation, long duration, OperationStats stats) {
		Map<String, Object> alertData = new HashMap<>();
		alertData.put("operation", operation);
		alertData.put("duration", duration);
		alertData.put("threshold", threshold.getThreshold(operation));
		alertData.put("stats", stats.getStats());

		alertSystem.raiseAlert("SLOW_OPERATION", alertData);
	}

	private void detectAnomalies(String operation, OperationStats stats) {
		double average = stats.getAverage();
		double stdDev = stats.getStandardDeviation();

		if (stdDev > average * THRESHOLD_MULTIPLIER) {
			alertSystem.raiseAlert("PERFORMANCE_ANOMALY", Map.of(
					"operation", operation,
					"average", average,
					"standardDeviation", stdDev));
		}
	}

	private void updateHistory(String operation, long duration) {
		long timestamp = System.currentTimeMillis();
		metricsHistory.put(timestamp, new PerformanceMetric(operation, duration));
		rollingAverage.update(duration);
	}

	private void cleanupHistory() {
		long cutoff = System.currentTimeMillis() - (CHECK_INTERVAL * HISTORY_SIZE);
		metricsHistory.headMap(cutoff).clear();
	}

	public Map<String, Object> getStats() {
		Map<String, Object> stats = new HashMap<>();
		operationStats.forEach((op, opStats) -> stats.put(op, opStats.getStats()));
		stats.put("rollingAverage", rollingAverage.getAverage());
		stats.put("historySize", metricsHistory.size());
		return stats;
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

	private static class OperationStats {
		private final String operation;
		private final AtomicLong count = new AtomicLong(0);
		private final AtomicLong totalTime = new AtomicLong(0);
		private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
		private final AtomicLong maxTime = new AtomicLong(0);
		private double m2 = 0.0; // Pour calcul écart-type
		private double mean = 0.0;

		OperationStats(String operation) {
			this.operation = operation;
		}

		synchronized void recordOperation(long duration) {
			long n = count.incrementAndGet();
			totalTime.addAndGet(duration);
			minTime.updateAndGet(current -> Math.min(current, duration));
			maxTime.updateAndGet(current -> Math.max(current, duration));

			// Algorithme de Welford pour calcul en ligne
			double delta = duration - mean;
			mean += delta / n;
			double delta2 = duration - mean;
			m2 += delta * delta2;
		}

		Map<String, Object> getStats() {
			long n = count.get();
			return Map.of(
					"operation", operation,
					"count", n,
					"average", getAverage(),
					"min", minTime.get(),
					"max", maxTime.get(),
					"standardDeviation", getStandardDeviation());
		}

		double getAverage() {
			return mean;
		}

		double getStandardDeviation() {
			long n = count.get();
			return n > 1 ? Math.sqrt(m2 / (n - 1)) : 0.0;
		}
	}

	private static class PerformanceMetric {
		final String operation;
		final long duration;
		final long timestamp;

		PerformanceMetric(String operation, long duration) {
			this.operation = operation;
			this.duration = duration;
			this.timestamp = System.currentTimeMillis();
		}
	}
}
