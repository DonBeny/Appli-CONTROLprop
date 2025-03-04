package org.orgaprop.test7.metrics.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.*;
import java.util.Map;
import org.orgaprop.test7.metrics.coordination.MetricsCoordinator;
import org.orgaprop.test7.metrics.quality.QualityManager;
import org.orgaprop.test7.metrics.normalization.MetricsNormalizer;
import org.orgaprop.test7.metrics.aggregation.MetricsAggregator;

public class MetricsAPI {
	private static final Logger logger = LoggerFactory.getLogger(MetricsAPI.class);
	private static volatile MetricsAPI instance;

	private final MetricsCoordinator coordinator;
	private final QualityManager qualityManager;
	private final MetricsNormalizer normalizer;
	private final MetricsAggregator aggregator;
	private final APIMetrics metrics;
	private final MetricsConfig config;
	private final ConfigModule apiConfig;

	private MetricsAPI() {
		this.config = MetricsConfig.getInstance();
		this.apiConfig = config.getModule("api");

		if (apiConfig == null) {
			logger.error("Configuration 'api' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		this.coordinator = MetricsCoordinator.getInstance();
		this.qualityManager = coordinator.getQualityManager();
		this.normalizer = coordinator.getMetricsNormalizer();
		this.aggregator = coordinator.getMetricsAggregator();
		this.metrics = new APIMetrics();
	}

	public static MetricsAPI getInstance() {
		if (instance == null) {
			synchronized (MetricsAPI.class) {
				if (instance == null) {
					instance = new MetricsAPI();
				}
			}
		}
		return instance;
	}

	// API Méthodes de base
	public void recordMetric(String name, double value, Map<String, Object> tags) {
		try {
			validateMetricInput(name, value, tags);

			Map<String, Object> normalizedData = normalizer.normalize(Map.of(
					"name", name,
					"value", value,
					"tags", tags));

			qualityManager.validateMetric(name, value);

			Map<String, Object> operationsConfig = (Map<String, Object>) apiConfig.getProperty("operations");
			int batchSize = (int) operationsConfig.get("batchSize");
			aggregator.addMetric(name, value, tags, batchSize);

			metrics.recordSuccess(MetricOperation.RECORD);
		} catch (Exception e) {
			metrics.recordError(MetricOperation.RECORD);
			handleError("Erreur lors de l'enregistrement de la métrique", e);
		}
	}

	private void validateMetricInput(String name, double value, Map<String, Object> tags) {
		Map<String, Object> validationConfig = (Map<String, Object>) apiConfig.getProperty("validation");
		boolean strictMode = (boolean) validationConfig.get("strictMode");
		int maxTagCount = (int) validationConfig.get("maxTagCount");
		int maxTagLength = (int) validationConfig.get("maxTagLength");

		if (strictMode) {
			if (tags.size() > maxTagCount) {
				throw new IllegalArgumentException("Nombre de tags trop élevé");
			}
			tags.forEach((k, v) -> {
				if (k.length() > maxTagLength) {
					throw new IllegalArgumentException("Tag trop long: " + k);
				}
			});
		}
	}

	private void handleError(String message, Exception e) {
		logger.error(message, e);

		Map<String, Object> metricsConfig = (Map<String, Object>) apiConfig.getProperty("metrics");
		int maxErrors = (int) metricsConfig.get("maxErrorsBeforeAlert");
		double errorThreshold = (double) metricsConfig.get("errorThreshold");

		if (metrics.getErrorRate() > errorThreshold) {
			coordinator.handleAlert("API_ERROR_THRESHOLD_EXCEEDED");
		}
	}

	// API Méthodes d'accès
	public Map<String, Object> getMetrics(MetricQuery query) {
		try {
			Map<String, Object> result = coordinator.getMetrics(query);
			metrics.recordSuccess(MetricOperation.GET);
			return result;
		} catch (Exception e) {
			metrics.recordError(MetricOperation.GET);
			handleError("Erreur lors de la récupération des métriques", e);
			return Map.of();
		}
	}

	// API Méthodes de gestion
	public void enableMetricType(String type) {
		try {
			coordinator.enableMetricType(type);
			metrics.recordSuccess(MetricOperation.ENABLE);
		} catch (Exception e) {
			metrics.recordError(MetricOperation.ENABLE);
			handleError("Erreur lors de l'activation du type de métrique", e);
		}
	}

	public void disableMetricType(String type) {
		try {
			coordinator.disableMetricType(type);
			metrics.recordSuccess(MetricOperation.DISABLE);
		} catch (Exception e) {
			metrics.recordError(MetricOperation.DISABLE);
			handleError("Erreur lors de la désactivation du type de métrique", e);
		}
	}

	private static class APIMetrics {
		private final Map<MetricOperation, AtomicInteger> successCount = new ConcurrentHashMap<>();
		private final Map<MetricOperation, AtomicInteger> errorCount = new ConcurrentHashMap<>();
		private final AtomicLong totalOperations = new AtomicLong(0);

		void recordSuccess(MetricOperation operation) {
			successCount.computeIfAbsent(operation, k -> new AtomicInteger()).incrementAndGet();
			totalOperations.incrementAndGet();
		}

		void recordError(MetricOperation operation) {
			errorCount.computeIfAbsent(operation, k -> new AtomicInteger()).incrementAndGet();
			totalOperations.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalOperations", totalOperations.get(),
					"successByOperation", new HashMap<>(successCount),
					"errorsByOperation", new HashMap<>(errorCount));
		}
	}

	private enum MetricOperation {
		RECORD, GET, ENABLE, DISABLE
	}

	public static class MetricQuery {
		private final String type;
		private final long startTime;
		private final long endTime;
		private final Map<String, Object> filters;

		private MetricQuery(Builder builder) {
			this.type = builder.type;
			this.startTime = builder.startTime;
			this.endTime = builder.endTime;
			this.filters = new HashMap<>(builder.filters);
		}

		public static class Builder {
			private String type;
			private long startTime;
			private long endTime;
			private final Map<String, Object> filters = new HashMap<>();

			public Builder setType(String type) {
				this.type = type;
				return this;
			}

			public Builder setTimeRange(long start, long end) {
				this.startTime = start;
				this.endTime = end;
				return this;
			}

			public Builder addFilter(String key, Object value) {
				this.filters.put(key, value);
				return this;
			}

			public MetricQuery build() {
				return new MetricQuery(this);
			}
		}
	}
}
