package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.Set;

public class CriticalResourceManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(CriticalResourceManager.class);

	private static final int DEFAULT_THREAD_POOL_SIZE = 2;
	private static final long MONITORING_INTERVAL = 30_000; // 30 secondes
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final Map<String, ResourceInfo> resources;
	private final Map<String, ResourceLock> resourceLocks;
	private final Set<String> criticalResources;
	private final ExecutorService monitoringExecutor;
	private final ScheduledExecutorService scheduler;
	private final AlertSystem alertSystem;
	private final ResourceMetrics metrics;
	private final AtomicBoolean isRunning;

	public CriticalResourceManager(AlertSystem alertSystem) {
		this.resources = new ConcurrentHashMap<>();
		this.resourceLocks = new ConcurrentHashMap<>();
		this.criticalResources = ConcurrentHashMap.newKeySet();
		this.monitoringExecutor = createMonitoringExecutor();
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.alertSystem = alertSystem;
		this.metrics = new ResourceMetrics();
		this.isRunning = new AtomicBoolean(true);

		startMonitoring();
	}

	public void registerResource(String resourceId, Resource resource, boolean isCritical) {
		ResourceInfo info = new ResourceInfo(resource, isCritical);
		resources.put(resourceId, info);
		resourceLocks.put(resourceId, new ResourceLock());

		if (isCritical) {
			criticalResources.add(resourceId);
			startResourceMonitoring(resourceId);
		}

		metrics.recordRegistration(resourceId, isCritical);
		logger.info("Resource enregistrée: {} (critique: {})", resourceId, isCritical);
	}

	public void monitorResource(String resourceId) {
		ResourceInfo info = resources.get(resourceId);
		if (info == null) {
			logger.warn("Tentative de monitoring d'une ressource inconnue: {}", resourceId);
			return;
		}

		ResourceLock lock = resourceLocks.get(resourceId);
		if (lock.isLocked()) {
			handleLockedResource(resourceId, lock);
		}

		try {
			checkResourceHealth(info);
		} catch (Exception e) {
			handleResourceError(resourceId, e);
		}
	}

	private void checkResourceHealth(ResourceInfo info) {
		if (!info.resource.isHealthy()) {
			handleUnhealthyResource(info);
		}
	}

	private void handleUnhealthyResource(ResourceInfo info) {
		metrics.recordHealthCheck(info.resource.getId(), false);

		if (info.isCritical) {
			handleCriticalResourceFailure(info);
		} else {
			handleNonCriticalResourceFailure(info);
		}
	}

	private void handleCriticalResourceFailure(ResourceInfo info) {
		String resourceId = info.resource.getId();
		logger.error("Ressource critique défaillante: {}", resourceId);

		Map<String, Object> alertData = Map.of(
				"resourceId", resourceId,
				"lastHealthCheck", info.lastHealthCheck,
				"failureCount", info.failureCount.incrementAndGet());

		alertSystem.raiseAlert("CRITICAL_RESOURCE_FAILURE", alertData);
		initiateResourceRecovery(info);
	}

	private void initiateResourceRecovery(ResourceInfo info) {
		monitoringExecutor.submit(() -> {
			try {
				info.resource.recover();
				metrics.recordRecovery(info.resource.getId(), true);
			} catch (Exception e) {
				metrics.recordRecovery(info.resource.getId(), false);
				logger.error("Échec de la récupération de la ressource: {}",
						info.resource.getId(), e);
			}
		});
	}

	@Override
	public void close() {
		isRunning.set(false);

		scheduler.shutdown();
		monitoringExecutor.shutdown();

		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
			if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				monitoringExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			scheduler.shutdownNow();
			monitoringExecutor.shutdownNow();
		}

		resources.clear();
		resourceLocks.clear();
		criticalResources.clear();
	}

	private static class ResourceInfo {
		final Resource resource;
		final boolean isCritical;
		final AtomicInteger failureCount;
		volatile long lastHealthCheck;

		ResourceInfo(Resource resource, boolean isCritical) {
			this.resource = resource;
			this.isCritical = isCritical;
			this.failureCount = new AtomicInteger(0);
			this.lastHealthCheck = System.currentTimeMillis();
		}
	}

	private static class ResourceMetrics {
		private final AtomicInteger totalResources = new AtomicInteger(0);
		private final AtomicInteger criticalResources = new AtomicInteger(0);
		private final AtomicInteger healthCheckFailures = new AtomicInteger(0);
		private final AtomicInteger successfulRecoveries = new AtomicInteger(0);

		void recordRegistration(String resourceId, boolean isCritical) {
			totalResources.incrementAndGet();
			if (isCritical) {
				criticalResources.incrementAndGet();
			}
		}

		void recordHealthCheck(String resourceId, boolean isHealthy) {
			if (!isHealthy) {
				healthCheckFailures.incrementAndGet();
			}
		}

		void recordRecovery(String resourceId, boolean success) {
			if (success) {
				successfulRecoveries.incrementAndGet();
			}
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalResources", totalResources.get(),
					"criticalResources", criticalResources.get(),
					"healthCheckFailures", healthCheckFailures.get(),
					"successfulRecoveries", successfulRecoveries.get());
		}
	}
}
