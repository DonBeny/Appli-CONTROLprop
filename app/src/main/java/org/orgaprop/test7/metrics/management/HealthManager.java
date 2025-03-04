package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class HealthManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(HealthManager.class);

	private static final long HEALTH_CHECK_INTERVAL = 5000; // 5 secondes
	private static final int CRITICAL_ERROR_THRESHOLD = 3;
	private static final double MEMORY_WARNING_THRESHOLD = 0.85;

	private final ScheduledExecutorService scheduler;
	private final Map<String, ComponentHealth> componentHealths;
	private final AtomicBoolean isRunning;
	private final HealthMetrics metrics;
	private final AlertManager alertManager;
	private final MemoryManager memoryManager;

	public HealthManager(AlertManager alertManager, MemoryManager memoryManager) {
		this.scheduler = createScheduler();
		this.componentHealths = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new HealthMetrics();
		this.alertManager = alertManager;
		this.memoryManager = memoryManager;

		startHealthMonitoring();
	}

	private void startHealthMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkSystemHealth,
				HEALTH_CHECK_INTERVAL,
				HEALTH_CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	public void registerComponent(String componentId, HealthCheck healthCheck) {
		ComponentHealth health = new ComponentHealth(componentId, healthCheck);
		componentHealths.put(componentId, health);
		metrics.recordComponentRegistration(componentId);
	}

	public void reportHealth(String componentId, HealthStatus status, Map<String, Object> details) {
		ComponentHealth health = componentHealths.get(componentId);
		if (health != null) {
			health.updateStatus(status, details);
			checkComponentHealth(health);
		}
	}

	private void checkSystemHealth() {
		try {
			SystemHealthSnapshot snapshot = collectHealthSnapshot();
			analyzeSystemHealth(snapshot);
			updateHealthMetrics(snapshot);
		} catch (Exception e) {
			handleHealthCheckError(e);
		}
	}

	private SystemHealthSnapshot collectHealthSnapshot() {
		Map<String, ComponentHealth> currentHealth = new HashMap<>(componentHealths);
		double memoryUsage = memoryManager.getCurrentUsageRatio();
		int errorCount = metrics.getErrorCount();

		return new SystemHealthSnapshot(currentHealth, memoryUsage, errorCount);
	}

	private void analyzeSystemHealth(SystemHealthSnapshot snapshot) {
		if (snapshot.hasMultipleCriticalErrors()) {
			handleSystemCriticalState(snapshot);
			return;
		}

		if (snapshot.memoryUsage > MEMORY_WARNING_THRESHOLD) {
			handleHighMemoryUsage(snapshot.memoryUsage);
		}

		snapshot.componentHealths.values().forEach(this::checkComponentHealth);
	}

	private void checkComponentHealth(ComponentHealth health) {
		if (health.getStatus() == HealthStatus.CRITICAL) {
			handleComponentCritical(health);
		} else if (health.getStatus() == HealthStatus.WARNING) {
			handleComponentWarning(health);
		}
	}

	private void handleSystemCriticalState(SystemHealthSnapshot snapshot) {
		Map<String, Object> alertData = new HashMap<>();
		alertData.put("errorCount", snapshot.errorCount);
		alertData.put("memoryUsage", snapshot.memoryUsage);
		alertData.put("criticalComponents", getCriticalComponents(snapshot));

		alertManager.raiseAlert("SYSTEM_CRITICAL", alertData);
		metrics.recordCriticalState();
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

	private static class ComponentHealth {
		private final String componentId;
		private final HealthCheck healthCheck;
		private volatile HealthStatus status;
		private Map<String, Object> details;
		private long lastUpdateTime;

		ComponentHealth(String componentId, HealthCheck healthCheck) {
			this.componentId = componentId;
			this.healthCheck = healthCheck;
			this.status = HealthStatus.UNKNOWN;
			this.details = Map.of();
			this.lastUpdateTime = System.currentTimeMillis();
		}

		synchronized void updateStatus(HealthStatus newStatus, Map<String, Object> newDetails) {
			this.status = newStatus;
			this.details = new HashMap<>(newDetails);
			this.lastUpdateTime = System.currentTimeMillis();
		}

		HealthStatus getStatus() {
			return status;
		}

		Map<String, Object> getDetails() {
			return new HashMap<>(details);
		}

		long getLastUpdateTime() {
			return lastUpdateTime;
		}
	}

	public enum HealthStatus {
		HEALTHY, WARNING, CRITICAL, UNKNOWN
	}

	private static class HealthMetrics {
		private final Map<String, AtomicInteger> errorsByComponent = new ConcurrentHashMap<>();
		private final AtomicInteger criticalStateCount = new AtomicInteger(0);
		private final AtomicInteger componentCount = new AtomicInteger(0);
		private final AtomicLong lastCriticalTime = new AtomicLong(0);

		void recordError(String componentId) {
			errorsByComponent.computeIfAbsent(componentId, k -> new AtomicInteger())
					.incrementAndGet();
		}

		void recordCriticalState() {
			criticalStateCount.incrementAndGet();
			lastCriticalTime.set(System.currentTimeMillis());
		}

		void recordComponentRegistration(String componentId) {
			componentCount.incrementAndGet();
		}

		int getErrorCount() {
			return errorsByComponent.values().stream()
					.mapToInt(AtomicInteger::get)
					.sum();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"componentCount", componentCount.get(),
					"criticalStateCount", criticalStateCount.get(),
					"errorsByComponent", new HashMap<>(errorsByComponent),
					"lastCriticalTime", lastCriticalTime.get());
		}
	}
}
