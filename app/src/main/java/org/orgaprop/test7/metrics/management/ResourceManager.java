package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ResourceManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);

	private static final long CHECK_INTERVAL = 5000; // 5 secondes
	private static final double MEMORY_THRESHOLD = 0.85; // 85% utilisation
	private static final int CPU_THRESHOLD = 80; // 80% utilisation

	private final ScheduledExecutorService scheduler;
	private final Map<String, ResourceMonitor> monitors;
	private final AtomicBoolean isRunning;
	private final ResourceMetrics metrics;
	private final AlertManager alertManager;

	public ResourceManager(AlertManager alertManager) {
		this.scheduler = createScheduler();
		this.monitors = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ResourceMetrics();
		this.alertManager = alertManager;

		startMonitoring();
	}

	private void startMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkResources,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	public void registerResource(String resourceId, ResourceType type, long capacity) {
		ResourceMonitor monitor = new ResourceMonitor(resourceId, type, capacity);
		monitors.put(resourceId, monitor);
		metrics.recordResourceRegistration(type);
	}

	public boolean allocateResource(String resourceId, long amount) {
		ResourceMonitor monitor = monitors.get(resourceId);
		if (monitor == null) {
			logger.warn("Ressource inconnue: {}", resourceId);
			return false;
		}

		try {
			boolean allocated = monitor.allocate(amount);
			if (allocated) {
				metrics.recordAllocation(monitor.getType(), amount);
				checkResourceThresholds(monitor);
			}
			return allocated;
		} catch (Exception e) {
			handleAllocationError(e, resourceId, amount);
			return false;
		}
	}

	private void checkResources() {
		try {
			monitors.values().forEach(this::checkResourceStatus);
			updateSystemMetrics();
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification des ressources", e);
		}
	}

	private void checkResourceStatus(ResourceMonitor monitor) {
		if (monitor.isOverloaded()) {
			handleResourceOverload(monitor);
		}

		if (monitor.needsOptimization()) {
			optimizeResource(monitor);
		}
	}

	private void handleResourceOverload(ResourceMonitor monitor) {
		Map<String, Object> alertData = Map.of(
				"resourceId", monitor.getId(),
				"type", monitor.getType(),
				"usage", monitor.getUsage(),
				"capacity", monitor.getCapacity());

		alertManager.raiseAlert("RESOURCE_OVERLOAD", alertData);
	}

	private void optimizeResource(ResourceMonitor monitor) {
		try {
			monitor.optimize();
			metrics.recordOptimization(monitor.getType());
		} catch (Exception e) {
			logger.error("Erreur lors de l'optimisation de {}", monitor.getId(), e);
		}
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

	private static class ResourceMonitor {
		private final String id;
		private final ResourceType type;
		private final long capacity;
		private final AtomicLong usage;
		private final AtomicLong peakUsage;
		private volatile long lastOptimization;

		ResourceMonitor(String id, ResourceType type, long capacity) {
			this.id = id;
			this.type = type;
			this.capacity = capacity;
			this.usage = new AtomicLong(0);
			this.peakUsage = new AtomicLong(0);
			this.lastOptimization = System.currentTimeMillis();
		}

		boolean allocate(long amount) {
			long currentUsage;
			do {
				currentUsage = usage.get();
				if (currentUsage + amount > capacity) {
					return false;
				}
			} while (!usage.compareAndSet(currentUsage, currentUsage + amount));

			updatePeakUsage(currentUsage + amount);
			return true;
		}

		private void updatePeakUsage(long currentUsage) {
			peakUsage.updateAndGet(peak -> Math.max(peak, currentUsage));
		}

		boolean isOverloaded() {
			return getUsageRatio() > MEMORY_THRESHOLD;
		}

		boolean needsOptimization() {
			return System.currentTimeMillis() - lastOptimization > 3600000; // 1 heure
		}

		void optimize() {
			// Logique d'optimisation spécifique au type de ressource
			lastOptimization = System.currentTimeMillis();
		}

		double getUsageRatio() {
			return (double) usage.get() / capacity;
		}

		// Getters
		String getId() {
			return id;
		}

		ResourceType getType() {
			return type;
		}

		long getUsage() {
			return usage.get();
		}

		long getCapacity() {
			return capacity;
		}
	}

	public enum ResourceType {
		MEMORY, CPU, DISK, NETWORK
	}
}
