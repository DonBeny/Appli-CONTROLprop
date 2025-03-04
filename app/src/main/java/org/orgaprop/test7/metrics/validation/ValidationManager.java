package org.orgaprop.test7.metrics.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ValidationManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ValidationManager.class);

	private final AtomicInteger validationFailures = new AtomicInteger(0);
	private final Map<String, ValidationRule> rules = new ConcurrentHashMap<>();
	private final ValidationMetrics metrics;
	private final AlertSystem alertSystem;

	public ValidationManager(AlertSystem alertSystem) {
		this.alertSystem = alertSystem;
		this.metrics = new ValidationMetrics();
		initializeDefaultRules();
	}

	public boolean validate(String metric, double value) {
		try {
			long startTime = System.nanoTime();
			boolean isValid = validateMetric(metric, value);
			metrics.recordValidation(System.nanoTime() - startTime);
			return isValid;
		} catch (Exception e) {
			handleValidationError(e, metric);
			return false;
		}
	}

	private boolean validateMetric(String metric, double value) {
		// Validation du nom de la métrique
		if (!validateMetricName(metric)) {
			return false;
		}

		// Validation de la valeur
		if (!validateMetricValue(value)) {
			return false;
		}

		// Application des règles spécifiques
		return applyValidationRules(metric, value);
	}

	private boolean validateMetricName(String metric) {
		if (metric == null || metric.trim().isEmpty()) {
			metrics.recordFailure("null_or_empty_metric");
			return false;
		}

		if (!metric.matches("^[a-zA-Z0-9._-]+$")) {
			metrics.recordFailure("invalid_metric_format");
			return false;
		}

		return true;
	}

	private boolean validateMetricValue(double value) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			metrics.recordFailure("invalid_value");
			return false;
		}
		return true;
	}

	private boolean applyValidationRules(String metric, double value) {
		for (ValidationRule rule : rules.values()) {
			if (!rule.validate(metric, value)) {
				metrics.recordFailure(rule.getName());
				return false;
			}
		}
		return true;
	}

	public void addValidationRule(ValidationRule rule) {
		rules.put(rule.getName(), rule);
		logger.info("Règle de validation ajoutée: {}", rule.getName());
	}

	private void handleValidationError(Exception e, String metric) {
		logger.error("Erreur de validation pour {}: {}", metric, e.getMessage());
		int failures = validationFailures.incrementAndGet();

		Map<String, Object> alertData = Map.of(
				"metric", metric,
				"failures", failures,
				"error", e.getMessage());

		alertSystem.raiseAlert("VALIDATION_ERROR", alertData);
	}

	@Override
	public void close() {
		rules.clear();
		metrics.reset();
	}

	private static class ValidationMetrics {
		private final AtomicLong totalValidations = new AtomicLong(0);
		private final AtomicLong totalFailures = new AtomicLong(0);
		private final Map<String, AtomicLong> failuresByType = new ConcurrentHashMap<>();
		private final AtomicLong totalValidationTime = new AtomicLong(0);

		void recordValidation(long duration) {
			totalValidations.incrementAndGet();
			totalValidationTime.addAndGet(duration);
		}

		void recordFailure(String type) {
			totalFailures.incrementAndGet();
			failuresByType.computeIfAbsent(type, k -> new AtomicLong())
					.incrementAndGet();
		}

		void reset() {
			totalValidations.set(0);
			totalFailures.set(0);
			failuresByType.clear();
			totalValidationTime.set(0);
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalValidations", totalValidations.get(),
					"totalFailures", totalFailures.get(),
					"failuresByType", new HashMap<>(failuresByType),
					"averageValidationTime", getAverageValidationTime());
		}

		private double getAverageValidationTime() {
			long validations = totalValidations.get();
			return validations > 0 ? (double) totalValidationTime.get() / validations : 0;
		}
	}
}
