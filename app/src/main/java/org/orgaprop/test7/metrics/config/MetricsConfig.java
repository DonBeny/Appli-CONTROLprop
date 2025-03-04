package org.orgaprop.test7.metrics.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;

public class MetricsConfig implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsConfig.class);
	private static final long serialVersionUID = 1L;

	private final Map<String, ConfigModule> modules;
	private final ConfigProperties globalProperties;
	private static MetricsConfig instance;

	private MetricsConfig() {
		this.modules = new ConcurrentHashMap<>();
		this.globalProperties = new ConfigProperties();
		initializeDefaultConfig();
	}

	public static synchronized MetricsConfig getInstance() {
		if (instance == null) {
			instance = new MetricsConfig();
		}
		return instance;
	}

	private void initializeDefaultConfig() {
		// Advanced Metrics Configuration
		addModule("advanced", new ConfigModule.Builder()
				.setProperty("maxEntrySize", 1024 * 1024) // 1MB
				.setProperty("maxTotalSize", 100 * 1024 * 1024) // 100MB
				.setProperty("maxEntries", 10_000)
				.setProperty("cleanupInterval", 5 * 60 * 1000) // 5 minutes
				.setProperty("backupInterval", 30 * 60 * 1000) // 30 minutes
				.setProperty("environment", "prod")
				.setProperty("thresholds", new HashMap<String, ThresholdConfig>())
				.setProperty("backup", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("location", "metrics/backup");
						put("retention", 7); // jours
						put("compression", true);
					}
				})
				.build());

		// Aggregation Configuration
		addModule("aggregation", new ConfigModule.Builder()
				.setProperty("batchSize", 100)
				.setProperty("flushInterval", 1 * 60 * 1000) // 1 minute
				.setProperty("compressionEnabled", true)
				.setProperty("bufferSize", 1000)
				.build());

		// API Configuration
		addModule("api", new ConfigModule.Builder()
				.setProperty("operations", new HashMap<String, Object>() {
					{
						put("maxRetries", 3);
						put("timeout", 30000L); // 30 secondes
						put("batchSize", 1000);
					}
				})
				.setProperty("validation", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("strictMode", false);
						put("maxValueLength", 1024);
						put("maxTagCount", 20);
						put("maxTagLength", 128);
					}
				})
				.setProperty("metrics", new HashMap<String, Object>() {
					{
						put("enablePerformanceTracking", true);
						put("retentionPeriod", 24 * 60 * 60 * 1000L); // 24 heures
						put("maxErrorsBeforeAlert", 100);
						put("errorThreshold", 0.1); // 10%
					}
				})
				.build());

		// Cache Configuration
		addModule("cache", new ConfigModule.Builder()
				.setProperty("maxEntries", 100)
				.setProperty("maxEntrySize", 1024 * 1024) // 1MB
				.setProperty("maxTotalSize", 10 * 1024 * 1024) // 10MB
				.setProperty("ttl", 30 * 60 * 1000) // 30 min
				.setProperty("cleanupInterval", 5 * 60 * 1000) // 5 minutes
				.setProperty("limits", new HashMap<String, Object>() {
					{
						put("minEntries", 1);
						put("maxEntries", 10000);
						put("minEntrySize", 1024L); // 1KB
						put("maxEntrySize", 100 * 1024 * 1024L); // 100MB
						put("minTTL", 1000L); // 1 seconde
						put("maxTTL", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.setProperty("validation", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("checkInterval", 1 * 60 * 1000); // 1 minute
						put("errorThreshold", 0.01); // 1%
						put("validateParameters", true);
						put("validateSerialization", true);
						put("validateObservability", true);
					}
				})
				.setProperty("observers", new ArrayList<String>())
				.setProperty("metadata", new HashMap<String, Object>())
				.build());

		// Configuration du PerformanceManager
		addModule("cache_performance", new ConfigModule.Builder()
				.setProperty("operations", new HashMap<String, Object>() {
					{
						put("totalOperations", 0L);
						put("statsRetentionSize", 100);
					}
				})
				.setProperty("thresholds", new HashMap<String, Object>() {
					{
						put("warningDuration", 500L); // 500ms
						put("criticalDuration", 1000L); // 1s
						put("maxOperationsPerSecond", 1000);
					}
				})
				.setProperty("monitoring", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("sampleRate", 1.0);
						put("logLevel", "WARN");
					}
				})
				.build());

		// Configuration des performances de compression
		addModule("compression", new ConfigModule.Builder()
				.setProperty("operation", new HashMap<String, Object>() {
					{
						put("maxDuration", 5 * 1000L); // 5 secondes
						put("maxInputSize", 10485760L); // 10MB
					}
				})
				.setProperty("compression", new HashMap<String, Object>() {
					{
						put("minRatio", 0.1); // 10% minimum
						put("maxRatio", 0.9); // 90% maximum
					}
				})
				.setProperty("resources", new HashMap<String, Object>() {
					{
						put("maxConcurrentOperations", 100);
						put("memoryThreshold", 0.85); // 85%
					}
				})
				.setProperty("cleanup", new HashMap<String, Object>() {
					{
						put("interval", 60 * 60 * 1000L); // 1 heure
						put("maxAge", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.setProperty("alerts", new HashMap<String, Object>() {
					{
						put("maxAlerts", 100);
						put("retentionPeriod", 60 * 60 * 1000L); // 1 heure
					}
				})
				.build());

		// Configuration de la coordination
		addModule("coordination", new ConfigModule.Builder()
				.setProperty("initialization", new HashMap<String, Object>() {
					{
						put("startupTimeout", 30000L); // 30 secondes
						put("managerStartOrder", Arrays.asList(
								"alert", "config", "memory", // Essentiels
								"validation", "cache", "persistence", "security", // Traitement
								"monitoring", "health", "performance", "resource", // Surveillance
								"process", "workflow", "event", "scheduler" // Processus
						));
					}
				})
				.setProperty("shutdown", new HashMap<String, Object>() {
					{
						put("gracePeriod", 10000L); // 10 secondes
						put("forceShutdownTimeout", 5000L); // 5 secondes
					}
				})
				.setProperty("health", new HashMap<String, Object>() {
					{
						put("checkInterval", 5000L); // 5 secondes
						put("retentionPeriod", 24 * 60 * 60 * 1000L); // 24 heures
						put("historySize", 1000);
					}
				})
				.setProperty("retention", new HashMap<String, Object>() {
					{
						put("maxAge", 30 * 24 * 60 * 60 * 1000L); // 30 jours
						put("maxSize", 1_000_000L);
						put("checkInterval", 60 * 60 * 1000L); // 1 heure
					}
				})
				.setProperty("monitoring", new HashMap<String, Object>() {
					{
						put("collectInterval", 10000L); // 10 secondes
						put("maxHistory", 1000);
					}
				})
				.build());

		// Configuration détaillée des métriques
		addModule("detailed_metrics", new ConfigModule.Builder()
				.setProperty("history", new HashMap<String, Object>() {
					{
						put("maxSize", 1000);
						put("maxHistorySizeBytes", 10 * 1024 * 1024L); // 10 MB
					}
				})
				.setProperty("memory", new HashMap<String, Object>() {
					{
						put("maxMemoryUsage", 100 * 1024 * 1024L); // 100MB
						put("cleanupThreshold", 0.9); // 90%
					}
				})
				.setProperty("sync", new HashMap<String, Object>() {
					{
						put("backupInterval", 30 * 60 * 1000L); // 30 minutes
						put("retentionPeriod", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.setProperty("aggregation", new HashMap<String, Object>() {
					{
						put("windowSize", 60 * 1000L); // 1 heure
						put("maxWindows", 24); // 24 fenêtres
					}
				})
				.build());

		// Configuration des filtres de métriques
		addModule("filters", new ConfigModule.Builder()
				.setProperty("time", new HashMap<String, Object>() {
					{
						put("defaultRange", 3600000L); // 1 heure par défaut
						put("maxRange", 7 * 24 * 60 * 60 * 1000L); // 7 jours maximum
					}
				})
				.setProperty("performance", new HashMap<String, Object>() {
					{
						put("defaultMaxResponseTime", 5000L); // 5 secondes
						put("warningThreshold", 1000L); // 1 seconde
						put("criticalThreshold", 3000L); // 3 secondes
					}
				})
				.setProperty("errors", new HashMap<String, Object>() {
					{
						put("defaultMinErrorCount", 0);
						put("warningThreshold", 10);
						put("criticalThreshold", 50);
					}
				})
				.setProperty("aggregation", new HashMap<String, Object>() {
					{
						put("maxBatchSize", 1000);
						put("enableDetailedStats", true);
						put("retentionPeriod", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.build());

		// Health Configuration
		addModule("health", new ConfigModule.Builder()
				.setProperty("checkInterval", 60)
				.setProperty("alertThreshold", 0.75)
				.setProperty("limits", new HashMap<String, Object>() {
					{
						// Seuils de validation
						put("minErrorRate", 0.0);
						put("maxErrorRate", 1000.0);
						put("minFailureRate", 0.0);
						put("maxFailureRate", 1.0);
						// Seuils d'avertissement
						put("warningErrorRate", 300.0);
						put("warningFailureRate", 0.5);
						// Facteurs restrictifs
						put("restrictiveErrorFactor", 0.1);
						put("restrictiveFailureFactor", 0.2);
					}
				})
				.setProperty("defaults", new HashMap<String, Object>() {
					{
						put("errorRate", 100.0);
						put("failureRate", 0.5);
						put("compatibilityThreshold", 0.5);
					}
				})
				.setProperty("securityLevels", new HashMap<String, Map<String, Double>>() {
					{
						put("STRICT", Map.of(
								"errorRate", 10.0,
								"failureRate", 0.1));
						put("MODERATE", Map.of(
								"errorRate", 100.0,
								"failureRate", 0.5));
						put("LENIENT", Map.of(
								"errorRate", 500.0,
								"failureRate", 0.8));
					}
				})
				.build());

		// Configuration des managers
		addModule("management", new ConfigModule.Builder()
				.setProperty("backup", new HashMap<String, Object>() {
					{
						put("interval", 3600000L); // 1 heure
						put("maxBackups", 10);
						put("compressionEnabled", true);
						put("backupLocation", "backups");
					}
				})
				.setProperty("lifecycle", new HashMap<String, Object>() {
					{
						put("startupTimeout", 30000L); // 30 secondes
						put("shutdownTimeout", 10000L); // 10 secondes
						put("healthCheckInterval", 5000L); // 5 secondes
					}
				})
				.setProperty("memory", new HashMap<String, Object>() {
					{
						put("maxHeapUsage", 0.85); // 85%
						put("cleanupThreshold", 0.75); // 75%
						put("monitoringInterval", 1000L); // 1 seconde
					}
				})
				.build());

		// Configuration du format métrique
		addModule("metric_format", new ConfigModule.Builder()
				.setProperty("types", new HashMap<String, Object>() {
					{
						put("COUNTER", new HashMap<String, Object>() {
							{
								put("valueType", "Number");
								put("allowNegative", false);
								put("validateValue", true);
							}
						});
						put("GAUGE", new HashMap<String, Object>() {
							{
								put("valueType", "Number");
								put("allowInfinite", false);
								put("allowNaN", false);
							}
						});
						put("TIMER", new HashMap<String, Object>() {
							{
								put("valueType", "Long");
								put("minValue", 0L);
								put("timeUnit", "MILLISECONDS");
							}
						});
						put("HISTOGRAM", new HashMap<String, Object>() {
							{
								put("valueType", "Number");
								put("buckets", Arrays.asList(0.1, 0.5, 0.9, 0.99));
							}
						});
					}
				})
				.setProperty("validation", new HashMap<String, Object>() {
					{
						put("required", Arrays.asList("name", "value", "type"));
						put("maxTagCount", 20);
						put("maxTagLength", 50);
						put("maxNameLength", 100);
					}
				})
				.build());

		// Configuration du monitoring
		addModule("monitoring", new ConfigModule.Builder()
				.setProperty("scheduler", new HashMap<String, Object>() {
					{
						put("threadPoolSize", Runtime.getRuntime().availableProcessors());
						put("checkInterval", 5000L); // 5 secondes
						put("gracePeriod", 30000L); // 30 secondes pour l'arrêt
					}
				})
				.setProperty("memory", new HashMap<String, Object>() {
					{
						put("warningThreshold", 0.85); // 85%
						put("criticalThreshold", 0.95); // 95%
						put("historySize", 100);
					}
				})
				.setProperty("performance", new HashMap<String, Object>() {
					{
						put("anomalyThreshold", 2.0); // écarts-types
						put("minSampleSize", 30);
						put("maxMetricHistory", 1000);
						put("warningThreshold", 1000L); // 1 seconde
						put("criticalThreshold", 5000L); // 5 secondes
					}
				})
				.setProperty("resources", new HashMap<String, Object>() {
					{
						put("maxConcurrentOperations", 100);
						put("cpuThreshold", 80); // 80%
						put("checkInterval", 30000L); // 30 secondes
					}
				})
				.setProperty("alerts", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("maxAlerts", 1000);
						put("retentionPeriod", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.build());

		// Configuration de normalisation détaillée
		addModule("normalization", new ConfigModule.Builder()
				.setProperty("enabled", true)
				.setProperty("precision", 2)
				.setProperty("timeUnit", "SECONDS")
				.setProperty("format", "JSON")
				.setProperty("rules", new HashMap<String, Object>() {
					{
						put("timestamp", new HashMap<String, Object>() {
							{
								put("required", true);
								put("type", "Long");
								put("transformations", Arrays.asList("normalizeTimestamp"));
							}
						});
						put("duration", new HashMap<String, Object>() {
							{
								put("required", false);
								put("type", "Long");
								put("transformations", Arrays.asList("normalizeDuration"));
							}
						});
						put("memory", new HashMap<String, Object>() {
							{
								put("required", false);
								put("type", "Long");
								put("transformations", Arrays.asList("normalizeMemory"));
							}
						});
						put("percentage", new HashMap<String, Object>() {
							{
								put("required", false);
								put("type", "Double");
								put("transformations", Arrays.asList("normalizePercentage"));
							}
						});
					}
				})
				.setProperty("execution", new HashMap<String, Object>() {
					{
						put("threadPoolSize", Runtime.getRuntime().availableProcessors());
						put("queueCapacity", 1000);
						put("timeout", 5000L);
					}
				})
				.build());

		// Performance Statistics Configuration
		addModule("performance_stats", new ConfigModule.Builder()
				.setProperty("thresholds", new HashMap<String, Object>() {
					{
						put("warning", 1000L); // 1 seconde pour warning
						put("error", 5000L); // 5 secondes pour error
						put("critical", 10000L); // 10 secondes pour critique
					}
				})
				.setProperty("monitoring", new HashMap<String, Object>() {
					{
						put("recentOperationsSize", 1000); // Taille du CircularFifoQueue
						put("movingAverageSize", 100); // Taille pour MovingAverage
						put("alertThreshold", 3); // Seuil pour les alertes consécutives
						put("checkTimeoutMs", 5000L); // Timeout pour vérification
					}
				})
				.setProperty("alerts", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("errorThreshold", 10); // Seuil d'événements critiques
					}
				})
				.build());

		// Configuration de la persistence des métriques
		addModule("persistence", new ConfigModule.Builder()
				.setProperty("storage", new HashMap<String, Object>() {
					{
						put("baseDirectory", "metrics");
						put("maxFileSize", 100 * 1024 * 1024L); // 100MB
						put("bufferSize", 8192); // 8KB
					}
				})
				.setProperty("batch", new HashMap<String, Object>() {
					{
						put("size", 1000);
						put("flushInterval", 30000L); // 30 secondes
						put("queueCapacity", 10000);
					}
				})
				.setProperty("retention", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("maxAge", 30 * 24 * 60 * 60 * 1000L); // 30 jours
						put("maxSize", 1024 * 1024 * 1024L); // 1GB
						put("cleanupInterval", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.setProperty("compression", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("algorithm", "GZIP");
						put("level", 6);
						put("minSize", 1024); // 1KB minimum
					}
				})
				.build());

		// Configuration de la qualité des métriques
		addModule("quality", new ConfigModule.Builder()
				.setProperty("validation", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("strictMode", true);
						put("expectedTypes", new HashMap<String, String>() {
							{
								put("timestamp", "java.lang.Long");
								put("duration", "java.lang.Long");
								put("count", "java.lang.Integer");
								put("ratio", "java.lang.Double");
							}
						});
					}
				})
				.setProperty("anomaly", new HashMap<String, Object>() {
					{
						put("detectionEnabled", true);
						put("threshold", 3.0); // 3 écarts-types
						put("minSampleSize", 30);
						put("windowSize", 100);
					}
				})
				.setProperty("processing", new HashMap<String, Object>() {
					{
						put("maxQueueSize", 1000);
						put("checkInterval", 60000L); // 1 minute
						put("threadPoolSize", Runtime.getRuntime().availableProcessors());
					}
				})
				.build());

		// Configuration du stockage des métriques
		addModule("storage", new ConfigModule.Builder()
				.setProperty("paths", new HashMap<String, Object>() {
					{
						put("baseDirectory", "metrics");
						put("indexFile", "metrics.idx");
						put("tempDirectory", "temp");
					}
				})
				.setProperty("retention", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("defaultPeriod", 30 * 24 * 60 * 60 * 1000L); // 30 jours
						put("maxSize", 100 * 1024 * 1024L); // 100MB
						put("cleanupInterval", 24 * 60 * 60 * 1000L); // 24 heures
					}
				})
				.setProperty("batch", new HashMap<String, Object>() {
					{
						put("size", 100);
						put("flushInterval", 30 * 1000L); // 30 seconde
						put("maxQueueSize", 10000);
						put("maxRetries", 3);
					}
				})
				.setProperty("compression", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("algorithm", "GZIP");
						put("level", 6);
						put("minSize", 1024L); // 1KB
					}
				})
				.setProperty("maintenance", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("cleanupInterval", 3600000L); // 1 heure
						put("maxFileAge", 7 * 24 * 60 * 60 * 1000L); // 7 jours
						put("maxTotalSize", 1024 * 1024 * 1024L); // 1GB
					}
				})
				.build());

		// Configuration du transport des métriques
		addModule("transport", new ConfigModule.Builder()
				.setProperty("batch", new HashMap<String, Object>() {
					{
						put("size", 100);
						put("flushInterval", 5000L); // 5 secondes
						put("maxQueueSize", 10000);
					}
				})
				.setProperty("retry", new HashMap<String, Object>() {
					{
						put("maxAttempts", 3);
						put("backoffInitial", 1000L); // 1 seconde
						put("backoffMultiplier", 2.0);
						put("maxBackoff", 60000L); // 1 minute
					}
				})
				.setProperty("compression", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("algorithm", "GZIP");
						put("level", 6);
					}
				})
				.setProperty("endpoints", new HashMap<String, Object>() {
					{
						put("timeout", 30000L); // 30 secondes
						put("maxConcurrent", 3);
						put("validateConnection", true);
					}
				})
				.build());

		// Security Configuration
		addModule("security", new ConfigModule.Builder()
				.setProperty("validationEnabled", true)
				.setProperty("maxRetries", 3)
				.setProperty("validation", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("strictMode", true);
						put("maxFailures", 5);
						put("lockoutDuration", 300000); // 5 minutes
						put("minPasswordLength", 8);
						put("requireSpecialChars", true);
						put("requireNumbers", true);
					}
				})
				.setProperty("encryption", new HashMap<String, Object>() {
					{
						put("algorithm", "AES");
						put("keySize", 256);
						put("saltLength", 16);
					}
				})
				.setProperty("metrics", new HashMap<String, Object>() {
					{
						put("maxUncompressedSize", 100 * 1024 * 1024L); // 100MB
						put("maxCompressedSize", 50 * 1024 * 1024L); // 50MB
						put("minCompressSize", 8L);
						put("maxKeyLength", 1024);
						put("maxStringLength", 5 * 1024 * 1024); // 5MB
						put("maxStructureDepth", 100);
						put("compressionRatios", new HashMap<String, Double>() {
							{
								put("min", 0.1); // 10% minimum
								put("max", 0.9); // 90% maximum
							}
						});
					}
				})
				.build());

		// Configuration de l'intégration système
		addModule("system_integration", new ConfigModule.Builder()
				.setProperty("collection", new HashMap<String, Object>() {
					{
						put("interval", 5000L); // 5 secondes
						put("historySize", 1000); // Nombre d'entrées d'historique
						put("batchSize", 100); // Taille des lots de données
					}
				})
				.setProperty("thresholds", new HashMap<String, Object>() {
					{
						put("memory", new HashMap<String, Object>() {
							{
								put("warningLevel", 0.85); // 85% utilisation mémoire
								put("criticalLevel", 0.95); // 95% utilisation mémoire
							}
						});
						put("cpu", new HashMap<String, Object>() {
							{
								put("warningLevel", 80.0); // 80% utilisation CPU
								put("criticalLevel", 90.0); // 90% utilisation CPU
							}
						});
					}
				})
				.setProperty("monitoring", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("retentionPeriod", 24 * 60 * 60 * 1000L); // 24 heures
						put("cleanupInterval", 60 * 60 * 1000L); // 1 heure
					}
				})
				.setProperty("collectors", new HashMap<String, Object>() {
					{
						put("types", Arrays.asList("MEMORY", "CPU", "THREAD", "IO"));
						put("errorThreshold", 10);
						put("retryInterval", 30000L); // 30 secondes
					}
				})
				.build());

		// Configuration des métriques UI
		addModule("ui_metrics", new ConfigModule.Builder()
				.setProperty("session", new HashMap<String, Object>() {
					{
						put("autoRestore", true);
						put("backupEnabled", true);
						put("retentionDays", 7);
					}
				})
				.setProperty("aggregation", new HashMap<String, Object>() {
					{
						put("layoutUsageEnabled", true);
						put("interactionTrackingEnabled", true);
						put("durationTrackingEnabled", true);
						put("bufferSize", 1000);
						put("flushInterval", 300000L); // 5 minutes
					}
				})
				.setProperty("thresholds", new HashMap<String, Object>() {
					{
						put("maxInteractionTime", 5000L); // 5 secondes
						put("maxLayoutChanges", 100); // par session
						put("warningThreshold", 0.75); // 75% du max
					}
				})
				.setProperty("cleanup", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("oldestAllowedData", 7 * 24 * 60 * 60 * 1000L); // 7 jours
						put("maxEntries", 10000);
					}
				})
				.build());

		// UI Performance Configuration
		addModule("ui_performance", new ConfigModule.Builder()
				.setProperty("thresholds", new HashMap<String, Long>() {
					{
						put("LAYOUT_RENDER_TIME", 16L); // ~60fps
						put("INTERACTION_RESPONSE_TIME", 100L);
						put("ANIMATION_FRAME_TIME", 16L); // ~60fps
						put("INPUT_PROCESSING_TIME", 50L);
						put("MEMORY_USAGE", 50L * 1024 * 1024L); // 50MB
					}
				})
				.setProperty("monitoring", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("sampleRate", 1.0);
						put("maxDataPoints", 1000);
					}
				})
				.build());

		// UI Performance Tracker Configuration
		addModule("ui_performance_tracker", new ConfigModule.Builder()
				.setProperty("thresholds", new HashMap<String, Long>() {
					{
						put("LAYOUT_CHANGE", 100L); // 100ms
						put("INPUT_PROCESSING", 50L); // 50ms
						put("ANIMATION", 16L); // ~60fps
						put("VALIDATION", 30L); // 30ms
						put("STATE_UPDATE", 20L); // 20ms
					}
				})
				.setProperty("monitoring", new HashMap<String, Object>() {
					{
						put("enabled", true);
						put("logLevel", "WARN");
						put("defaultThreshold", 50L);
					}
				})
				.build());

		// Configuration du format unifié des métriques
		addModule("unified_format", new ConfigModule.Builder()
				.setProperty("validation", new HashMap<String, Object>() {
					{
						put("requiredFields", Arrays.asList("timestamp", "type", "value"));
						put("typeConstraints", new HashMap<String, String>() {
							{
								put("timestamp", "java.lang.Long");
								put("value", "java.lang.Object");
								put("tags", "java.util.Map");
							}
						});
						put("valueConstraints", new HashMap<String, List<String>>() {
							{
								put("timestamp", Arrays.asList("not_null", "positive"));
								put("value", Arrays.asList("not_null"));
							}
						});
					}
				})
				.setProperty("versioning", new HashMap<String, Object>() {
					{
						put("defaultVersion", 1);
						put("minVersion", 1);
						put("maxVersion", 2);
					}
				})
				.setProperty("metadata", new HashMap<String, Object>() {
					{
						put("maxSize", 1024);
						put("allowedTypes", Arrays.asList(
								"java.lang.String",
								"java.lang.Number",
								"java.lang.Boolean"));
					}
				})
				.build());

		// Configuration de la visualisation
		addModule("visualization", new ConfigModule.Builder()
				.setProperty("chart", new HashMap<String, Object>() {
					{
						put("colors", new int[] {
								0xFF4CAF50, 0xFF2196F3, 0xFFFF9800,
								0xFFE91E63, 0xFF9C27B0, 0xFF795548
						});
						put("lineWidth", 4f);
						put("antiAlias", true);
					}
				})
				.setProperty("layout", new HashMap<String, Object>() {
					{
						put("padding", new int[] { 16, 16, 16, 16 });
						put("legendPosition", "RIGHT");
						put("showGrid", true);
						put("backgroundColor", 0xFFFFFFFF);
					}
				})
				.setProperty("transformers", new HashMap<String, Object>() {
					{
						put("enabledTypes", Arrays.asList(
								"LINE", "BAR", "PIE", "HEATMAP"));
						put("defaultType", "LINE");
						put("batchSize", 100);
					}
				})
				.setProperty("performance", new HashMap<String, Object>() {
					{
						put("maxQueueSize", 1000);
						put("threadPoolSize", Runtime.getRuntime().availableProcessors());
						put("updateInterval", 1000L);
					}
				})
				.build());

		// Global Configuration
		globalProperties.setProperty("environment", "production");
		globalProperties.setProperty("metricsEnabled", true);
		globalProperties.setProperty("logLevel", "INFO");
	}

	public void addModule(String name, ConfigModule module) {
		modules.put(name, module);
		logger.info("Module de configuration ajouté: {}", name);
	}

	public ConfigModule getModule(String name) {
		return modules.get(name);
	}

	public Object getGlobalProperty(String key) {
		return globalProperties.getProperty(key);
	}

	public void setGlobalProperty(String key, Object value) {
		globalProperties.setProperty(key, value);
		logger.debug("Propriété globale mise à jour: {} = {}", key, value);
	}

	public static class ConfigModule implements Serializable {
		private final ConfigProperties properties;

		private ConfigModule(Builder builder) {
			this.properties = builder.properties;
		}

		public Object getProperty(String key) {
			return properties.getProperty(key);
		}

		public static class Builder {
			private final ConfigProperties properties = new ConfigProperties();

			public Builder setProperty(String key, Object value) {
				properties.setProperty(key, value);
				return this;
			}

			public ConfigModule build() {
				return new ConfigModule(this);
			}
		}
	}

	private static class ConfigProperties implements Serializable {
		private final Map<String, Object> properties = new ConcurrentHashMap<>();

		public void setProperty(String key, Object value) {
			properties.put(key, value);
		}

		public Object getProperty(String key) {
			return properties.get(key);
		}
	}
}
