package org.orgaprop.test7.metrics.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.HashMap;
import java.lang.management.ManagementFactory;

public class ResourceMonitor implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);

	private static final double MEMORY_THRESHOLD = 0.85;
	private static final int MAX_CONCURRENT_OPERATIONS = 100;
	private static final long CHECK_INTERVAL = 30_000; // 30 secondes
	private static final int CPU_THRESHOLD = 80; // pourcentage

	private final AtomicInteger activeOperations;
	private final ScheduledExecutorService scheduler;
	private final AlertSystem alertSystem;
	private final AtomicBoolean isRunning;
	private final ResourceMetrics metrics;
	private final Map<String, ResourceUsage> resourceUsage;

	public ResourceMonitor(AlertSystem alertSystem) {
		this.activeOperations = new AtomicInteger(0);
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "resource-monitor");
			t.setDaemon(true);
			return t;
		});
		this.alertSystem = alertSystem;
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ResourceMetrics();
		this.resourceUsage = new ConcurrentHashMap<>();

		startMonitoring();
	}

	private void startMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkResources,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	public boolean isOverloaded() {
		return activeOperations.get() >= MAX_CONCURRENT_OPERATIONS ||
				getMemoryUsage() > MEMORY_THRESHOLD ||
				getCpuUsage() > CPU_THRESHOLD;
	}

	public void registerResource(String resourceId, ResourceType type) {
		resourceUsage.put(resourceId, new ResourceUsage(type));
		metrics.recordRegistration(type);
	}

	public void recordResourceUse(String resourceId, long amount) {
		ResourceUsage usage = resourceUsage.get(resourceId);
		if (usage != null) {
			usage.recordUsage(amount);
			metrics.recordUsage(usage.type, amount);
			checkResourceThresholds(resourceId, usage);
		}
	}

	private void checkResources() {
		try {
			double memoryUsage = getMemoryUsage();
			double cpuUsage = getCpuUsage();

			Map<String, Object> resourceStats = collectResourceStats();

			if (memoryUsage > MEMORY_THRESHOLD || cpuUsage > CPU_THRESHOLD) {
				handleResourcePressure(memoryUsage, cpuUsage);
			}

			metrics.updateStats(memoryUsage, cpuUsage, resourceStats);
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification des ressources", e);
		}
	}

	private void handleResourcePressure(double memoryUsage, double cpuUsage) {
		Map<String, Object> alertData = new HashMap<>();
		alertData.put("memoryUsage", memoryUsage);
		alertData.put("cpuUsage", cpuUsage);
		alertData.put("activeOperations", activeOperations.get());

		alertSystem.raiseAlert("RESOURCE_PRESSURE", alertData);
		triggerResourceOptimization();
	}

	private void triggerResourceOptimization() {
		// Libération des ressources non essentielles
		cleanupUnusedResources();
		System.gc();
	}

	private double getMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		return (double) usedMemory / maxMemory;
	}

	private double getCpuUsage() {
		return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
	}

	private Map<String, Object> collectResourceStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("memoryUsage", getMemoryUsage());
		stats.put("cpuUsage", getCpuUsage());
		stats.put("activeOperations", activeOperations.get());
		stats.putAll(metrics.getStats());
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
			cleanupResources();
		}
	}

	private static class ResourceUsage {
		final ResourceType type;
		final AtomicLong totalUsage = new AtomicLong(0);
		final AtomicInteger activeUses = new AtomicInteger(0);
		volatile long lastUsed = System.currentTimeMillis();

		ResourceUsage(ResourceType type) {
			this.type = type;
		}

		void recordUsage(long amount) {
			totalUsage.addAndGet(amount);
			activeUses.incrementAndGet();
			lastUsed = System.currentTimeMillis();
		}
	}

	private static class ResourceMetrics {
		private final Map<ResourceType, AtomicLong> usageByType = new ConcurrentHashMap<>();
		private final AtomicInteger totalResources = new AtomicInteger(0);

		void recordRegistration(ResourceType type) {
			totalResources.incrementAndGet();
			usageByType.putIfAbsent(type, new AtomicLong(0));
		}

		void recordUsage(ResourceType type, long amount) {
			usageByType.computeIfAbsent(type, k -> new AtomicLong())
					.addAndGet(amount);
		}

		void updateStats(double memoryUsage, double cpuUsage, Map<String, Object> resourceStats) {
			// Mise à jour des statistiques
		}

		Map<String, Object> getStats() {
			Map<String, Object> stats = new HashMap<>();
			stats.put("totalResources", totalResources.get());
			usageByType.forEach((type, usage) -> stats.put("usage_" + type.name(), usage.get()));
			return stats;
		}
	}

	public enum ResourceType {
		MEMORY,
		CPU,
		NETWORK,
		DISK,
		THREAD
	}
}
