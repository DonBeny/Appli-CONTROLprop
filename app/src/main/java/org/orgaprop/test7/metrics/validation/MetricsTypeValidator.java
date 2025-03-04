package org.orgaprop.test7.metrics.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Predicate;

public class MetricsTypeValidator {
	private static final Logger logger = LoggerFactory.getLogger(MetricsTypeValidator.class);

	private final Map<String, TypeDefinition> typeDefinitions;
	private final Map<Class<?>, TypeValidator> validators;
	private final ValidationMetrics metrics;

	public MetricsTypeValidator() {
		this.typeDefinitions = new ConcurrentHashMap<>();
		this.validators = new ConcurrentHashMap<>();
		this.metrics = new ValidationMetrics();
		initializeDefaultValidators();
	}

	private void initializeDefaultValidators() {
		registerValidator(Number.class, value -> !Double.isNaN(((Number) value).doubleValue())
				&& !Double.isInfinite(((Number) value).doubleValue()));
		registerValidator(String.class, value -> value != null && !((String) value).isEmpty());
		registerValidator(Map.class, value -> value != null && !((Map<?, ?>) value).isEmpty());
	}

	public void registerValidator(Class<?> type, Predicate<Object> validator) {
		validators.put(type, new TypeValidator(validator));
	}

	public void registerTypeDefinition(String metricName, TypeDefinition definition) {
		typeDefinitions.put(metricName, definition);
	}

	public boolean validate(String metricName, Object value) {
		try {
			long startTime = System.nanoTime();
			boolean isValid = performValidation(metricName, value);
			metrics.recordValidation(System.nanoTime() - startTime);
			return isValid;
		} catch (Exception e) {
			metrics.recordError();
			logger.error("Erreur lors de la validation de {}: {}", metricName, e.getMessage());
			return false;
		}
	}

	private boolean performValidation(String metricName, Object value) {
		// Validation basique
		if (value == null) {
			metrics.recordFailure("null_value");
			return false;
		}

		// Validation du type spécifique
		TypeDefinition definition = typeDefinitions.get(metricName);
		if (definition != null && !definition.validate(value)) {
			metrics.recordFailure("type_mismatch");
			return false;
		}

		// Validation générique par type
		TypeValidator validator = validators.get(value.getClass());
		if (validator != null && !validator.validate(value)) {
			metrics.recordFailure("validation_failed");
			return false;
		}

		return true;
	}

	public Map<String, Object> getValidationStats() {
		return metrics.getStats();
	}

	private static class TypeValidator {
		private final Predicate<Object> validationRule;

		TypeValidator(Predicate<Object> validationRule) {
			this.validationRule = validationRule;
		}

		boolean validate(Object value) {
			return validationRule.test(value);
		}
	}

	private static class TypeDefinition {
		private final Class<?> expectedType;
		private final boolean isRequired;
		private final Predicate<Object> customValidation;

		public TypeDefinition(Class<?> expectedType, boolean isRequired, Predicate<Object> customValidation) {
			this.expectedType = expectedType;
			this.isRequired = isRequired;
			this.customValidation = customValidation;
		}

		boolean validate(Object value) {
			if (value == null) {
				return !isRequired;
			}
			return expectedType.isInstance(value) &&
					(customValidation == null || customValidation.test(value));
		}
	}

	private static class ValidationMetrics {
		private final Map<String, AtomicInteger> failuresByType = new ConcurrentHashMap<>();
		private final AtomicLong totalValidations = new AtomicLong(0);
		private final AtomicLong totalErrors = new AtomicLong(0);
		private final AtomicLong totalValidationTime = new AtomicLong(0);

		void recordValidation(long duration) {
			totalValidations.incrementAndGet();
			totalValidationTime.addAndGet(duration);
		}

		void recordFailure(String type) {
			failuresByType.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
		}

		void recordError() {
			totalErrors.incrementAndGet();
		}

		Map<String, Object> getStats() {
			Map<String, Object> stats = new HashMap<>();
			stats.put("totalValidations", totalValidations.get());
			stats.put("totalErrors", totalErrors.get());
			stats.put("failuresByType", new HashMap<>(failuresByType));
			stats.put("averageValidationTime", getAverageValidationTime());
			return stats;
		}

		private double getAverageValidationTime() {
			long validations = totalValidations.get();
			return validations > 0 ? (double) totalValidationTime.get() / validations : 0;
		}
	}
}
