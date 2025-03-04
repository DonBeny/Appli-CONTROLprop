package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.time.Duration;

public class ConfigurationManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

	private static final long CONFIG_CHECK_INTERVAL = Duration.ofMinutes(5).toMillis();
	private static final int MAX_CONFIGS = 100;

	private final Map<String, ConfigurationHolder> configurations;
	private final ScheduledExecutorService scheduler;
	private final ConfigurationValidator validator;
	private final ConfigurationStorage storage;
	private final AtomicBoolean isRunning;
	private final AlertManager alertManager;

	public ConfigurationManager(AlertManager alertManager) {
		this.configurations = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.validator = new ConfigurationValidator();
		this.storage = new ConfigurationStorage();
		this.isRunning = new AtomicBoolean(true);
		this.alertManager = alertManager;

		startConfigurationMonitoring();
	}

	public void registerConfiguration(String configId, Object config) {
		validateConfig(configId, config);
		ConfigurationHolder holder = new ConfigurationHolder(config);
		configurations.put(configId, holder);
		storage.saveConfiguration(configId, config);
		notifyConfigurationChange(configId, config);
	}

	public <T> T getConfiguration(String configId, Class<T> type) {
		ConfigurationHolder holder = configurations.get(configId);
		if (holder == null) {
			return loadDefaultConfiguration(configId, type);
		}
		return type.cast(holder.getConfig());
	}

	public void updateConfiguration(String configId, Object newConfig) {
		validateConfig(configId, newConfig);
		ConfigurationHolder holder = configurations.get(configId);

		if (holder != null) {
			holder.setConfig(newConfig);
			storage.saveConfiguration(configId, newConfig);
			notifyConfigurationChange(configId, newConfig);
		}
	}

	private void startConfigurationMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkConfigurations,
				CONFIG_CHECK_INTERVAL,
				CONFIG_CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void checkConfigurations() {
		configurations.forEach((id, holder) -> {
			try {
				if (!validator.isValid(holder.getConfig())) {
					handleInvalidConfiguration(id, holder);
				}
			} catch (Exception e) {
				handleConfigurationError(id, e);
			}
		});
	}

	private void validateConfig(String configId, Object config) {
		if (config == null) {
			throw new IllegalArgumentException("La configuration ne peut pas être null");
		}
		if (!validator.isValid(config)) {
			throw new IllegalArgumentException("Configuration invalide pour " + configId);
		}
	}

	private <T> T loadDefaultConfiguration(String configId, Class<T> type) {
		try {
			T defaultConfig = storage.loadConfiguration(configId, type);
			if (defaultConfig != null) {
				registerConfiguration(configId, defaultConfig);
				return defaultConfig;
			}
			throw new ConfigurationException("Configuration non trouvée: " + configId);
		} catch (Exception e) {
			throw new ConfigurationException("Erreur chargement configuration: " + configId, e);
		}
	}

	private void handleInvalidConfiguration(String configId, ConfigurationHolder holder) {
		Map<String, Object> alertData = Map.of(
				"configId", configId,
				"timestamp", System.currentTimeMillis(),
				"config", holder.getConfig());
		alertManager.raiseAlert("INVALID_CONFIGURATION", alertData);
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
			storage.close();
		}
	}

	private static class ConfigurationHolder {
		private volatile Object config;
		private final long creationTime;
		private volatile long lastUpdateTime;

		ConfigurationHolder(Object config) {
			this.config = config;
			this.creationTime = System.currentTimeMillis();
			this.lastUpdateTime = this.creationTime;
		}

		Object getConfig() {
			return config;
		}

		void setConfig(Object newConfig) {
			this.config = newConfig;
			this.lastUpdateTime = System.currentTimeMillis();
		}

		long getAge() {
			return System.currentTimeMillis() - creationTime;
		}

		long getLastUpdateAge() {
			return System.currentTimeMillis() - lastUpdateTime;
		}
	}
}
