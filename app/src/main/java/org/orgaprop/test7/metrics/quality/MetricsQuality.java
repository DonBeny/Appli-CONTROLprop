package org.orgaprop.test7.metrics.quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MetricsQuality {
	private static final Logger logger = LoggerFactory.getLogger(MetricsQuality.class);

	private final QualityRules rules;
	private final AnomalyDetection anomalyDetection;
	private final MetricsValidation validation;
	private final ConfigModule qualityConfig;

	public MetricsQuality(ConfigModule config) {
		this.qualityConfig = config;
		Map<String, Object> validationConfig = (Map<String, Object>) qualityConfig.getProperty("validation");
		Map<String, Object> anomalyConfig = (Map<String, Object>) qualityConfig.getProperty("anomaly");

		this.rules = new QualityRules((Map<String, String>) validationConfig.get("expectedTypes"));
		this.anomalyDetection = new AnomalyDetection(
				(double) anomalyConfig.get("threshold"),
				(int) anomalyConfig.get("minSampleSize"));
		this.validation = new MetricsValidation((boolean) validationConfig.get("strictMode"));
	}

	public ValidationResult validateMetric(String type, Object value) {
		try {
			rules.validateType(type, value);
			validation.validateFormat(value);
			return ValidationResult.success();
		} catch (Exception e) {
			return ValidationResult.failure(e.getMessage());
		}
	}

	public boolean detectAnomaly(MetricData data) {
		return anomalyDetection.isAnomaly(data);
	}

	private static class QualityRules {
		private final Map<String, Class<?>> expectedTypes = new ConcurrentHashMap<>();

		QualityRules() {
			initializeDefaultRules();
		}

		private void initializeDefaultRules() {
			expectedTypes.put("timestamp", Long.class);
			expectedTypes.put("duration", Long.class);
			expectedTypes.put("count", Integer.class);
			expectedTypes.put("ratio", Double.class);
		}

		void validateType(String type, Object value) {
			Class<?> expectedType = expectedTypes.get(type);
			if (expectedType != null && !expectedType.isInstance(value)) {
				throw new IllegalArgumentException(
						String.format("Type invalide pour %s: attendu %s, reçu %s",
								type, expectedType.getSimpleName(), value.getClass().getSimpleName()));
			}
		}
	}

	private static class AnomalyDetection {
		private final Map<String, MovingStats> metricStats = new ConcurrentHashMap<>();

		boolean isAnomaly(MetricData data) {
			MovingStats stats = metricStats.computeIfAbsent(
					data.getName(),
					k -> new MovingStats());

			return stats.isAnomaly(data.getValue());
		}
	}

	private static class MovingStats {
		private double mean = 0.0;
		private double m2 = 0.0;
		private long count = 0;

		synchronized boolean isAnomaly(double value) {
			updateStats(value);
			if (count < 30)
				return false; // Pas assez de données

			double stdDev = Math.sqrt(m2 / (count - 1));
			return Math.abs(value - mean) > ANOMALY_THRESHOLD * stdDev;
		}

		private void updateStats(double value) {
			count++;
			double delta = value - mean;
			mean += delta / count;
			m2 += delta * (value - mean);
		}
	}

	private static class MetricsValidation {
		void validateFormat(Object value) {
			if (value == null) {
				throw new IllegalArgumentException("La valeur ne peut pas être null");
			}

			if (value instanceof Number) {
				validateNumber((Number) value);
			}
		}

		private void validateNumber(Number value) {
			if (value instanceof Double || value instanceof Float) {
				double doubleValue = value.doubleValue();
				if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
					throw new IllegalArgumentException("Valeur numérique invalide");
				}
			}
		}
	}

	public static class ValidationResult {
		private final boolean valid;
		private final String message;

		private ValidationResult(boolean valid, String message) {
			this.valid = valid;
			this.message = message;
		}

		public static ValidationResult success() {
			return new ValidationResult(true, null);
		}

		public static ValidationResult failure(String message) {
			return new ValidationResult(false, message);
		}

		public boolean isValid() {
			return valid;
		}

		public String getMessage() {
			return message;
		}
	}

	public static class MetricData {
		private final String name;
		private final double value;

		public MetricData(String name, double value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public double getValue() {
			return value;
		}
	}
}
