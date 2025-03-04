package org.orgaprop.test7.metrics.format;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifiedMetricFormat implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(UnifiedMetricFormat.class);
	private static final long serialVersionUID = 1L;

	private final Map<String, Object> data;
	private final MetricsConfig config;
	private final ConfigModule unifiedConfig;

	public UnifiedMetricFormat(Map<String, Object> data) {
		this.config = MetricsConfig.getInstance();
		this.unifiedConfig = config.getModule("unified_format");

		if (unifiedConfig == null) {
			logger.error("Configuration 'unified_format' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		validateFormat(data);
		this.data = new HashMap<>(data);
	}

	private void validateFormat(Map<String, Object> data) {
		Map<String, Object> validationConfig = (Map<String, Object>) unifiedConfig.getProperty("validation");
		List<String> requiredFields = (List<String>) validationConfig.get("requiredFields");
		Map<String, String> typeConstraints = (Map<String, String>) validationConfig.get("typeConstraints");

		for (String field : requiredFields) {
			if (!data.containsKey(field)) {
				throw new IllegalArgumentException("Champ requis manquant: " + field);
			}
		}

		for (Map.Entry<String, String> constraint : typeConstraints.entrySet()) {
			Object value = data.get(constraint.getKey());
			if (value != null) {
				validateType(constraint.getKey(), value, constraint.getValue());
			}
		}

		Map<String, Object> metadataConfig = (Map<String, Object>) unifiedConfig.getProperty("metadata");
		validateMetadata(data, metadataConfig);
	}

	private void validateMetadata(Map<String, Object> data, Map<String, Object> metadataConfig) {
		int maxSize = (int) metadataConfig.get("maxSize");
		if (data.size() > maxSize) {
			throw new IllegalArgumentException("Taille des données excessive: " + data.size() + " > " + maxSize);
		}

		List<String> allowedTypes = (List<String>) metadataConfig.get("allowedTypes");
		for (Object value : data.values()) {
			if (value != null && !isAllowedType(value.getClass().getName(), allowedTypes)) {
				throw new IllegalArgumentException("Type non autorisé: " + value.getClass().getName());
			}
		}
	}

	private boolean isAllowedType(String typeName, List<String> allowedTypes) {
		return allowedTypes.contains(typeName);
	}

	private void validateType(String key, Object value, String expectedType) {
		try {
			Class<?> expectedClass = Class.forName(expectedType);
			if (!expectedClass.isInstance(value)) {
				throw new IllegalArgumentException(
						String.format("Type invalide pour %s : attendu %s, reçu %s",
								key, expectedClass.getSimpleName(), value.getClass().getSimpleName()));
			}
		} catch (ClassNotFoundException e) {
			logger.error("Type de contrainte invalide: {}", expectedType);
		}
	}

	public enum MetricType {
		COUNTER, // Valeurs incrémentales
		GAUGE, // Valeurs instantanées
		HISTOGRAM, // Distribution
		SUMMARY, // Résumé statistique
		EVENT, // Événement
		STATE // État
	}

	public static class ValidationRules implements Serializable {
		private final Set<String> requiredFields;
		private final Map<String, Class<?>> typeConstraints;
		private final Map<String, List<String>> valueConstraints;

		private ValidationRules(Builder builder) {
			this.requiredFields = new HashSet<>(builder.requiredFields);
			this.typeConstraints = new HashMap<>(builder.typeConstraints);
			this.valueConstraints = new HashMap<>(builder.valueConstraints);
		}

		public static class Builder {
			private final Set<String> requiredFields = new HashSet<>();
			private final Map<String, Class<?>> typeConstraints = new HashMap<>();
			private final Map<String, List<String>> valueConstraints = new HashMap<>();

			public Builder addRequiredField(String field) {
				requiredFields.add(field);
				return this;
			}

			public Builder addTypeConstraint(String field, Class<?> type) {
				typeConstraints.put(field, type);
				return this;
			}

			public Builder addValueConstraint(String field, String constraint) {
				valueConstraints.computeIfAbsent(field, k -> new ArrayList<>())
						.add(constraint);
				return this;
			}

			public ValidationRules build() {
				return new ValidationRules(this);
			}
		}
	}

	public static class Builder {
		private String id;
		private MetricType type;
		private Object value;
		private final Map<String, Object> metadata = new HashMap<>();
		private long timestamp = Instant.now().toEpochMilli();
		private int version = 1;
		private ValidationRules validationRules = new ValidationRules.Builder().build();

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withType(MetricType type) {
			this.type = type;
			return this;
		}

		public Builder withValue(Object value) {
			this.value = value;
			return this;
		}

		public Builder addMetadata(String key, Object value) {
			this.metadata.put(key, value);
			return this;
		}

		public Builder withTimestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder withVersion(int version) {
			this.version = version;
			return this;
		}

		public Builder withValidationRules(ValidationRules rules) {
			this.validationRules = rules;
			return this;
		}

		public UnifiedMetricFormat build() {
			return new UnifiedMetricFormat(this);
		}
	}

	// Getters
	public String getId() {
		return id;
	}

	public MetricType getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	public Map<String, Object> getMetadata() {
		return new HashMap<>(metadata);
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return String.format("UnifiedMetricFormat{id='%s', type=%s, value=%s, metadata=%s, timestamp=%d, version=%d}",
				id, type, value, metadata, timestamp, version);
	}
}
