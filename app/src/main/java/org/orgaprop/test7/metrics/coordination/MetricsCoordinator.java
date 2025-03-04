package org.orgaprop.test7.metrics.coordination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.orgaprop.test7.metrics.management.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.nio.file.Path;

public class MetricsCoordinator implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsCoordinator.class);

	private final AtomicBoolean isRunning;
	private final CoordinatorMetrics metrics;
	private final MetricsConfig config;
	private final ConfigModule coordinationConfig;

	public MetricsCoordinator(Path storagePath) {
		this.config = MetricsConfig.getInstance();
		this.coordinationConfig = config.getModule("coordination");

		if (coordinationConfig == null) {
			logger.error("Configuration 'coordination' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		this.isRunning = new AtomicBoolean(true);
		this.metrics = new CoordinatorMetrics();

		initializeManagers(storagePath);
	}

	private void initializeManagers(Path storagePath) {
		Map<String, Object> initConfig = (Map<String, Object>) coordinationConfig.getProperty("initialization");
		List<String> startOrder = (List<String>) initConfig.get("managerStartOrder");
		long startupTimeout = (long) initConfig.get("startupTimeout");

		for (String managerName : startOrder) {
			initializeManager(managerName, storagePath);
		}

		initializeCoordination();
	}

	private RetentionPolicy createRetentionPolicy() {
		Map<String, Object> retentionConfig = (Map<String, Object>) coordinationConfig.getProperty("retention");

		return new RetentionPolicy.Builder()
				.setMaxAge(Duration.ofMillis((long) retentionConfig.get("maxAge")))
				.setMaxSize((long) retentionConfig.get("maxSize"))
				.setCheckInterval(Duration.ofMillis((long) retentionConfig.get("checkInterval")))
				.build();
	}

	private void initializeCoordination() {
		registerHealthChecks();
		registerEventHandlers();
		registerMetricCollectors();
		configureValidationRules();
		setupResourceMonitoring();
		startScheduledTasks();
	}

	private void registerHealthChecks() {
		healthManager.registerComponent("cache", createCacheHealthCheck());
		healthManager.registerComponent("persistence", createPersistenceHealthCheck());
		healthManager.registerComponent("security", createSecurityHealthCheck());
		// ... autres composants
	}

	private void registerEventHandlers() {
		eventManager.registerHandler("metrics.update", this::handleMetricsUpdate);
		eventManager.registerHandler("system.alert", this::handleSystemAlert);
		// ... autres handlers
	}

	private void registerMetricCollectors() {
		monitoringManager.registerCollector("memory", new MemoryMetricCollector(memoryManager));
		monitoringManager.registerCollector("cache", new CacheMetricCollector(cacheManager));
		// ... autres collectors
	}

	public Map<String, Object> getGlobalMetrics() {
		Map<String, Object> globalMetrics = new HashMap<>();
		globalMetrics.put("memory", memoryManager.getStats());
		globalMetrics.put("cache", cacheManager.getStats());
		globalMetrics.put("performance", performanceManager.getStats());
		globalMetrics.put("health", healthManager.getStats());
		globalMetrics.put("coordination", metrics.getStats());
		return globalMetrics;
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			logger.info("Arrêt ordonné du MetricsCoordinator...");

			Map<String, Object> shutdownConfig = (Map<String, Object>) coordinationConfig.getProperty("shutdown");
			long gracePeriod = (long) shutdownConfig.get("gracePeriod");
			long forceTimeout = (long) shutdownConfig.get("forceShutdownTimeout");

			List<String> startOrder = (List<String>) ((Map<String, Object>) coordinationConfig
					.getProperty("initialization")).get("managerStartOrder");

			// Fermeture dans l'ordre inverse de l'initialisation
			for (int i = startOrder.size() - 1; i >= 0; i--) {
				String managerName = startOrder.get(i);
				AutoCloseable manager = getManagerByName(managerName);
				closeManager(managerName, manager);
			}

			logger.info("Arrêt du MetricsCoordinator terminé");
		}
	}

	private void closeManager(String name, AutoCloseable manager) {
		try {
			manager.close();
			metrics.recordManagerClosure(name, true);
		} catch (Exception e) {
			logger.error("Erreur lors de la fermeture du manager {}: {}", name, e.getMessage());
			metrics.recordManagerClosure(name, false);
		}
	}

	private static class CoordinatorMetrics {
		private final Map<String, AtomicInteger> managerInitCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> managerCloseCount = new ConcurrentHashMap<>();
		private final AtomicInteger totalManagers = new AtomicInteger(0);
		private final AtomicInteger activeManagers = new AtomicInteger(0);

		void recordManagerInitialization(String managerName) {
			incrementCounter(managerInitCount, managerName);
			totalManagers.incrementAndGet();
			activeManagers.incrementAndGet();
		}

		void recordManagerClosure(String managerName, boolean success) {
			if (success) {
				incrementCounter(managerCloseCount, managerName);
				activeManagers.decrementAndGet();
			}
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalManagers", totalManagers.get(),
					"activeManagers", activeManagers.get(),
					"initializations", new HashMap<>(managerInitCount),
					"closures", new HashMap<>(managerCloseCount));
		}
	}
}
