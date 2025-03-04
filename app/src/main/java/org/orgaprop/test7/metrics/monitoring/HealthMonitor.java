package org.orgaprop.test7.metrics.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.HashMap;
import org.apache.commons.collections4.queue.CircularFifoQueue;

public class HealthMonitor implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);

	private static final long CHECK_INTERVAL = 30_000; // 30 secondes
	private static final int HISTORY_SIZE = 100;
	private static final double MEMORY_WARNING = 0.85; // 85%
	private static final double MEMORY_CRITICAL = 0.95; // 95%
	private static final int ERROR_THRESHOLD = 10;

	private final ScheduledExecutorService scheduler;
	private final CircularFifoQueue<HealthStatus> healthHistory;
	private final AtomicBoolean isRunning;
	private final HealthMetrics metrics;
	private final AlertSystem alertSystem;

	public HealthMonitor(AlertSystem alertSystem) {
		this.scheduler = createScheduler();
		this.healthHistory = new CircularFifoQueue<>(HISTORY_SIZE);
		this.alertSystem = alertSystem;
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new HealthMetrics();
	}

	private ScheduledExecutorService createScheduler() {
		return Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "health-monitor");
			t.setDaemon(true);
			t.setPriority(Thread.MAX_PRIORITY);
			return t;
		});
	}

	public void start() {
		scheduler.scheduleAtFixedRate(
				this::performHealthCheck,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void performHealthCheck() {
		try {
			HealthStatus status = collectHealthData();
			analyzeHealth(status);
			updateMetrics(status);
			healthHistory.add(status);
		} catch (Exception e) {
			handleHealthCheckError(e);
		}
	}

	public void checkEnvironment() {
		try {
			validateMemory();
			validateThreads();
			validateResources();
			logger.info("Vérification environnement réussie");
		} catch (Exception e) {
			logger.error("Échec vérification environnement", e);
			throw new EnvironmentCheckException("Échec vérification", e);
		}
	}

	private void checkHealth() {
		if (!isRunning.get())
			return;

		try {
			HealthStatus status = collectHealthStatus();
			healthHistory.add(status);
			analyzeHealth(status);
			metrics.updateMetrics(status);
		} catch (Exception e) {
			handleHealthCheckError(e);
		}
	}

	private HealthStatus collectHealthStatus() {
		double memoryUsage = getMemoryUsage();
		int threadCount = Thread.activeCount();
		Map<String, Object> resourceStats = collectResourceStats();

		return new HealthStatus(memoryUsage, threadCount, resourceStats);
	}

	private void analyzeHealth(HealthStatus status) {
		if (status.isMemoryCritical()) {
			handleCriticalMemory(status);
		} else if (status.isMemoryWarning()) {
			handleMemoryWarning(status);
		}

		if (status.hasExcessiveThreads()) {
			handleThreadWarning(status);
		}

		analyzeTrends();
	}

	private void analyzeTrends() {
		if (healthHistory.size() < 3)
			return;

		double avgMemoryUsage = calculateAverageMemoryUsage();
		if (avgMemoryUsage > MEMORY_WARNING_THRESHOLD) {
			alertSystem.raiseAlert("MEMORY_TREND_WARNING", Map.of(
					"averageUsage", avgMemoryUsage,
					"threshold", MEMORY_WARNING_THRESHOLD));
		}
	}

	private double getMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		return (double) usedMemory / maxMemory;
	}

	private Map<String, Object> collectResourceStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("memoryUsage", getMemoryUsage());
		stats.put("threadCount", Thread.activeCount());
		stats.put("systemLoad", getSystemLoad());
		return stats;
	}

	private double getSystemLoad() {
		return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
	}

	public Map<String, Object> getHealthStats() {
		return metrics.getStats();
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

	private static class HealthMetrics {
		private final AtomicInteger warningCount = new AtomicInteger(0);
		private final AtomicInteger criticalCount = new AtomicInteger(0);
		private final AtomicLong lastWarningTime = new AtomicLong(0);
		private final AtomicLong lastCriticalTime = new AtomicLong(0);

		void updateMetrics(HealthStatus status) {
			if (status.isMemoryWarning()) {
				warningCount.incrementAndGet();
				lastWarningTime.set(System.currentTimeMillis());
			}
			if (status.isMemoryCritical()) {
				criticalCount.incrementAndGet();
				lastCriticalTime.set(System.currentTimeMillis());
			}
		}

		Map<String, Object> getStats() {
			return Map.of(
					"warningCount", warningCount.get(),
					"criticalCount", criticalCount.get(),
					"lastWarningTime", lastWarningTime.get(),
					"lastCriticalTime", lastCriticalTime.get());
		}
	}
}
