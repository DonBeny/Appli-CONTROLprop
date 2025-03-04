package org.orgaprop.test7.metrics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.Properties;
import java.io.IOException;

public class ConfigurationManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

	private final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock(true);
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
	private final Properties configProperties;
	private volatile MetricsConfig currentConfig;
	private final ConfigValidator validator;
	private final String environment;

	public ConfigurationManager(String environment) {
		this.environment = environment;
		this.configProperties = new Properties();
		this.validator = new ConfigValidator();
		this.currentConfig = MetricsConfig.getDefaultConfig(environment);
	}

	public void initialize() throws ConfigurationException {
		if (!isInitialized.compareAndSet(false, true)) {
			throw new IllegalStateException("ConfigurationManager déjà initialisé");
		}

		try {
			loadConfiguration();
			validateConfiguration();
		} catch (Exception e) {
			isInitialized.set(false);
			throw new ConfigurationException("Erreur d'initialisation de la configuration", e);
		}
	}

	public void updateConfig(MetricsConfig newConfig) {
		configLock.writeLock().lock();
		try {
			if (validator.validate(newConfig)) {
				MetricsConfig oldConfig = currentConfig;
				currentConfig = newConfig;
				notifyConfigChanged(oldConfig, newConfig);
			} else {
				throw new InvalidConfigurationException("Configuration invalide");
			}
		} finally {
			configLock.writeLock().unlock();
		}
	}

	public MetricsConfig getCurrentConfig() {
		configLock.readLock().lock();
		try {
			return currentConfig;
		} finally {
			configLock.readLock().unlock();
		}
	}

	public void addChangeListener(ConfigChangeListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public void removeChangeListener(ConfigChangeListener listener) {
		listeners.remove(listener);
	}

	private void loadConfiguration() throws IOException {
		String configPath = getConfigPath();
		try {
			configProperties.load(Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(configPath));
			updateFromProperties();
		} catch (Exception e) {
			logger.warn("Impossible de charger la configuration, utilisation des valeurs par défaut", e);
		}
	}

	private void updateFromProperties() {
		MetricsConfig.Builder builder = new MetricsConfig.Builder()
				.setEnvironment(environment)
				.setMaxEntrySize(getLongProperty("metrics.maxEntrySize",
						MetricsConfig.DEFAULT_MAX_ENTRY_SIZE))
				.setMaxTotalSize(getLongProperty("metrics.maxTotalSize",
						MetricsConfig.DEFAULT_MAX_TOTAL_SIZE))
				.setMemoryThreshold(getDoubleProperty("metrics.memoryThreshold",
						MetricsConfig.DEFAULT_MEMORY_THRESHOLD))
				.setMaxEntries(getIntProperty("metrics.maxEntries",
						MetricsConfig.DEFAULT_MAX_ENTRIES));

		updateConfig(builder.build());
	}

	private String getConfigPath() {
		return String.format("config/metrics-%s.properties", environment.toLowerCase());
	}

	private void validateConfiguration() {
		if (!validator.validate(currentConfig)) {
			throw new InvalidConfigurationException("Configuration invalide après chargement");
		}
	}

	private void notifyConfigChanged(MetricsConfig oldConfig, MetricsConfig newConfig) {
		for (ConfigChangeListener listener : listeners) {
			try {
				listener.onConfigurationChanged(oldConfig, newConfig);
			} catch (Exception e) {
				logger.error("Erreur lors de la notification du changement de configuration", e);
			}
		}
	}

	private long getLongProperty(String key, long defaultValue) {
		try {
			return Long.parseLong(configProperties.getProperty(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			logger.warn("Valeur invalide pour {}, utilisation de la valeur par défaut", key);
			return defaultValue;
		}
	}

	private int getIntProperty(String key, int defaultValue) {
		try {
			return Integer.parseInt(configProperties.getProperty(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			logger.warn("Valeur invalide pour {}, utilisation de la valeur par défaut", key);
			return defaultValue;
		}
	}

	private double getDoubleProperty(String key, double defaultValue) {
		try {
			return Double.parseDouble(configProperties.getProperty(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException e) {
			logger.warn("Valeur invalide pour {}, utilisation de la valeur par défaut", key);
			return defaultValue;
		}
	}

	@Override
	public void close() {
		listeners.clear();
		configProperties.clear();
		isInitialized.set(false);
	}
}
