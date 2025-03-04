package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemoryManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MemoryManager.class);

	// Seuils de mémoire
	private static final double WARNING_THRESHOLD = 0.85; // 85%
	private static final double CRITICAL_THRESHOLD = 0.95; // 95%

	// Statistiques et monitoring
	private final AtomicLong totalSize = new AtomicLong(0);
	private final AtomicLong peakMemoryUsage = new AtomicLong(0);
	private final AtomicInteger cleanupCount = new AtomicInteger(0);
	private final AtomicLong allocationCount = new AtomicLong(0);

	private final long maxMemory;
	private final MemoryUsagePredictor predictor;
	private final MemoryMetricsCollector metricsCollector;
	private final MemoryCircuitBreaker circuitBreaker;

	private final ScheduledExecutorService monitor;
	private final MetricsConfig config;
	private final ConfigModule managementConfig;

	public MemoryManager(long maxMemory) {
		this.maxMemory = maxMemory;
		this.predictor = new MemoryUsagePredictor();
		this.metricsCollector = new MemoryMetricsCollector();
		this.circuitBreaker = new MemoryCircuitBreaker();
		this.config = MetricsConfig.getInstance();
		this.managementConfig = config.getModule("management");

		if (managementConfig == null) {
			logger.error("Configuration 'management' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		this.monitor = Executors.newScheduledThreadPool(1);
		startMonitoring();
	}

	private void startMonitoring() {
		Map<String, Object> memoryConfig = (Map<String, Object>) managementConfig.getProperty("memory");
		long interval = (long) memoryConfig.get("monitoringInterval");

		monitor.scheduleAtFixedRate(
				this::checkMemoryUsage,
				0,
				interval,
				TimeUnit.MILLISECONDS);
	}

	private void checkMemoryUsage() {
		Map<String, Object> memoryConfig = (Map<String, Object>) managementConfig.getProperty("memory");
		double maxHeapUsage = (double) memoryConfig.get("maxHeapUsage");
		double cleanupThreshold = (double) memoryConfig.get("cleanupThreshold");

		Runtime runtime = Runtime.getRuntime();
		double usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (double) runtime.maxMemory();

		if (usedMemory > maxHeapUsage) {
			logger.warn("Utilisation mémoire critique: {}%", usedMemory * 100);
			triggerEmergencyCleanup();
		} else if (usedMemory > cleanupThreshold) {
			logger.info("Déclenchement nettoyage préventif: {}%", usedMemory * 100);
			triggerPreventiveCleanup();
		}
	}

	public boolean canAllocate(long size) {
		if (!circuitBreaker.isAvailable()) {
			return false;
		}
		return totalSize.get() + size <= maxMemory;
	}

	public void checkMemoryUsage(long requestedSize) {
		double currentUsage = getCurrentUsageRatio();
		updatePeakUsage(currentUsage);

		if (predictor.predictCriticalUsage(currentUsage)) {
			performPreemptiveCleanup();
		}

		if (currentUsage > CRITICAL_THRESHOLD) {
			handleCriticalMemory(currentUsage, requestedSize);
		} else if (currentUsage > WARNING_THRESHOLD) {
			handleWarningMemory(currentUsage);
		}
	}

	public void recordAllocation(long size) {
		if (canAllocate(size)) {
			totalSize.addAndGet(size);
			allocationCount.incrementAndGet();
			metricsCollector.recordAllocation(size);
		} else {
			handleAllocationFailure(size);
		}
	}

	public void recordDeallocation(long size) {
		long newTotal = totalSize.addAndGet(-size);
		if (newTotal < 0) {
			logger.error("Détection d'usage mémoire négatif, réinitialisation");
			totalSize.set(0);
		}
		metricsCollector.recordDeallocation(size);
	}

	private void handleCriticalMemory(double usage, long requestedSize) {
		circuitBreaker.recordFailure();
		performEmergencyCleanup();

		Map<String, Object> alertData = new HashMap<>();
		alertData.put("usage", usage);
		alertData.put("requested", requestedSize);
		alertData.put("totalSize", totalSize.get());
		alertData.put("maxMemory", maxMemory);

		logger.error("Mémoire critique: {}%, demande: {} bytes",
				String.format("%.2f", usage * 100), requestedSize);

		if (usage > CRITICAL_THRESHOLD) {
			throw new MemoryLimitExceededException("Seuil critique dépassé: " + usage);
		}
	}

	public Map<String, Object> getStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("currentUsage", getCurrentUsageRatio());
		stats.put("peakUsage", (double) peakMemoryUsage.get() / maxMemory);
		stats.put("totalAllocations", allocationCount.get());
		stats.put("cleanupCount", cleanupCount.get());
		stats.put("circuitBreakerStatus", circuitBreaker.getStatus());
		stats.putAll(metricsCollector.getMetrics());
		return stats;
	}

	@Override
	public void close() {
		try {
			performCleanup();
		} finally {
			metricsCollector.reset();
			predictor.reset();
			circuitBreaker.reset();
		}
	}

	// Méthodes privées utilitaires
	private double getCurrentUsageRatio() {
		return (double) totalSize.get() / maxMemory;
	}

	private void updatePeakUsage(double currentUsage) {
		peakMemoryUsage.updateAndGet(current -> Math.max(current, (long) (currentUsage * maxMemory)));
	}
}