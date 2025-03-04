package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class PerformanceManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(PerformanceManager.class);

	private static final long CHECK_INTERVAL = 5000; // 5 secondes
	private static final double CPU_THRESHOLD = 0.80; // 80%
	private static final double MEMORY_THRESHOLD = 0.85; // 85%
	private static final long HISTORY_RETENTION = 3600_000; // 1 heure

	private final Map<String, OperationStats> operationStats;
	private final ConcurrentNavigableMap<Long, PerformanceSnapshot> performanceHistory;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final PerformanceMetrics metrics;
	private final AlertManager alertManager;

	public PerformanceManager(AlertManager alertManager) {
		this.operationStats = new ConcurrentHashMap<>();
		this.performanceHistory = new ConcurrentSkipListMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new PerformanceMetrics();
		this.alertManager = alertManager;

		startMonitoring();
	}

	public void recordOperation(String operationType, long duration) {
		if (!isRunning.get()) {
			logger.warn("Tentative d'enregistrement alors que le manager est arrêté");
			return;
		}

		try {
			OperationStats stats = operationStats.computeIfAbsent(
					operationType,
					k -> new OperationStats(operationType));
			stats.recordOperation(duration);
			metrics.recordOperation(operationType, duration);

			checkPerformanceThresholds(operationType, stats);
		} catch (Exception e) {
			handleRecordingError(e, operationType);
		}
	}

	private void startMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkSystemPerformance,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void checkSystemPerformance() {
		try {
			PerformanceSnapshot snapshot = capturePerformanceSnapshot();
			analyzePerformance(snapshot);
			updateHistory(snapshot);
			cleanupHistory();
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification des performances", e);
		}
	}

	private PerformanceSnapshot capturePerformanceSnapshot() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		double cpuUsage = calculateCpuUsage();

		return new PerformanceSnapshot(
				System.currentTimeMillis(),
				cpuUsage,
				(double) (totalMemory - freeMemory) / totalMemory,
				new HashMap<>(operationStats));
	}

	private void analyzePerformance(PerformanceSnapshot snapshot) {
		if (snapshot.cpuUsage > CPU_THRESHOLD) {
			handleHighCpuUsage(snapshot);
		}
		if (snapshot.memoryUsage > MEMORY_THRESHOLD) {
			handleHighMemoryUsage(snapshot);
		}
		detectPerformanceAnomalies(snapshot);
	}

	private void updateHistory(PerformanceSnapshot snapshot) {
		performanceHistory.put(snapshot.timestamp, snapshot);
	}

	private void cleanupHistory() {
		long cutoff = System.currentTimeMillis() - HISTORY_RETENTION;
		performanceHistory.headMap(cutoff).clear();
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
		}
	}

	private static class OperationStats {
		private final String operationType;
		private final AtomicLong count = new AtomicLong(0);
		private final AtomicLong totalDuration = new AtomicLong(0);
		private final AtomicLong maxDuration = new AtomicLong(0);
		private double m2 = 0.0; // Pour le calcul de l'écart-type
		private double mean = 0.0;

		OperationStats(String operationType) {
			this.operationType = operationType;
		}

		synchronized void recordOperation(long duration) {
			long n = count.incrementAndGet();
			totalDuration.addAndGet(duration);
			maxDuration.updateAndGet(current -> Math.max(current, duration));

			// Algorithme de Welford pour le calcul en ligne
			double delta = duration - mean;
			mean += delta / n;
			double delta2 = duration - mean;
			m2 += delta * delta2;
		}

		Map<String, Object> getStats() {
			long n = count.get();
			return Map.of(
					"operationType", operationType,
					"count", n,
					"averageDuration", getAverageDuration(),
					"maxDuration", maxDuration.get(),
					"standardDeviation", getStandardDeviation());
		}

		double getAverageDuration() {
			long n = count.get();
			return n > 0 ? (double) totalDuration.get() / n : 0;
		}

		double getStandardDeviation() {
			long n = count.get();
			return n > 1 ? Math.sqrt(m2 / (n - 1)) : 0;
		}
	}

	private static class PerformanceMetrics {
		private final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> totalDurations = new ConcurrentHashMap<>();
		private final AtomicInteger anomalyCount = new AtomicInteger(0);
		private final AtomicLong totalOperations = new AtomicLong(0);

		void recordOperation(String type, long duration) {
			operationCounts.computeIfAbsent(type, k -> new AtomicLong()).incrementAndGet();
			totalDurations.computeIfAbsent(type, k -> new AtomicLong()).addAndGet(duration);
			totalOperations.incrementAndGet();
		}

		void recordAnomaly() {
			anomalyCount.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalOperations", totalOperations.get(),
					"operationCounts", new HashMap<>(operationCounts),
					"averageDurations", calculateAverageDurations(),
					"anomalyCount", anomalyCount.get());
		}

		private Map<String, Double> calculateAverageDurations() {
			Map<String, Double> averages = new HashMap<>();
			operationCounts.forEach((type, count) -> {
				long total = totalDurations.getOrDefault(type, new AtomicLong()).get();
				averages.put(type, count.get() > 0 ? (double) total / count.get() : 0);
			});
			return averages;
		}
	}
}
