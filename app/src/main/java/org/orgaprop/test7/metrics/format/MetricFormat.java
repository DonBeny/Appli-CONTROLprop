package org.orgaprop.test7.metrics.format;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricFormat {
	private static final Logger logger = LoggerFactory.getLogger(MetricFormat.class);

	private final String name;
	private final Object value;
	private final MetricType type;
	private final Map<String, String> tags;
	private final long timestamp;
	private final MetricsConfig config;
	private final ConfigModule formatConfig;

	private MetricFormat(Builder builder) {
		this.name = builder.name;
		this.value = builder.value;
		this.type = builder.type;
		this.tags = new HashMap<>(builder.tags);
		this.timestamp = builder.timestamp;
		this.config = MetricsConfig.getInstance();
		this.formatConfig = config.getModule("metric_format");

		if (formatConfig == null) {
			logger.error("Configuration 'metric_format' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}
		validate();
	}

	public enum MetricType {
		COUNTER, // Valeurs incrémentales
		GAUGE, // Valeurs instantanées
		HISTOGRAM, // Distribution de valeurs
		TIMER, // Durées
		SUMMARY // Statistiques agrégées
	}

	private void validate() {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Le nom de la métrique est requis");
		}
		if (value == null) {
			throw new IllegalArgumentException("La valeur de la métrique est requise");
		}
		if (type == null) {
			throw new IllegalArgumentException("Le type de métrique est requis");
		}
		validateValue();
	}

	private void validateValue() {
		switch (type) {
			case COUNTER:
			case GAUGE:
				if (!(value instanceof Number)) {
					throw new IllegalArgumentException("Les compteurs et jauges doivent être numériques");
				}
				if (value instanceof Double && (Double.isNaN((Double) value) || Double.isInfinite((Double) value))) {
					throw new IllegalArgumentException("Valeur numérique invalide");
				}
				break;
			case TIMER:
				if (!(value instanceof Long)) {
					throw new IllegalArgumentException("Les durées doivent être en Long");
				}
				if ((Long) value < 0) {
					throw new IllegalArgumentException("Les durées ne peuvent pas être négatives");
				}
				break;
		}
	}

	private void validateInput(String name, Object value, MetricType type, Map<String, String> tags) {
		Map<String, Object> validationConfig = (Map<String, Object>) formatConfig.getProperty("validation");
		List<String> required = (List<String>) validationConfig.get("required");
		int maxTagCount = (int) validationConfig.get("maxTagCount");
		int maxTagLength = (int) validationConfig.get("maxTagLength");
		int maxNameLength = (int) validationConfig.get("maxNameLength");

		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Le nom est requis");
		}

		if (name.length() > maxNameLength) {
			throw new IllegalArgumentException("Nom trop long: " + name.length() + " > " + maxNameLength);
		}

		if (tags.size() > maxTagCount) {
			throw new IllegalArgumentException("Trop de tags: " + tags.size() + " > " + maxTagCount);
		}

		for (Map.Entry<String, String> tag : tags.entrySet()) {
			if (tag.getKey().length() > maxTagLength) {
				throw new IllegalArgumentException("Tag trop long: " + tag.getKey());
			}
		}

		Map<String, Object> typeConfig = (Map<String, Object>) ((Map<String, Object>) formatConfig.getProperty("types"))
				.get(type.name());

		validateValueType(value, (String) typeConfig.get("valueType"), type);
	}

	public static class Builder {
		private String name;
		private Object value;
		private MetricType type;
		private final Map<String, Object> tags = new HashMap<>();
		private long timestamp = Instant.now().toEpochMilli();

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withValue(Object value) {
			this.value = value;
			return this;
		}

		public Builder withType(MetricType type) {
			this.type = type;
			return this;
		}

		public Builder addTag(String key, Object value) {
			this.tags.put(key, value);
			return this;
		}

		public Builder withTags(Map<String, Object> tags) {
			this.tags.putAll(tags);
			return this;
		}

		public Builder withTimestamp(long timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public MetricFormat build() {
			return new MetricFormat(this);
		}
	}

	// Getters
	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public MetricType getType() {
		return type;
	}

	public Map<String, Object> getTags() {
		return new HashMap<>(tags);
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MetricFormat that = (MetricFormat) o;
		return timestamp == that.timestamp &&
				Objects.equals(name, that.name) &&
				Objects.equals(value, that.value) &&
				type == that.type &&
				Objects.equals(tags, that.tags);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value, type, tags, timestamp);
	}

	@Override
	public String toString() {
		return String.format("MetricFormat{name='%s', value=%s, type=%s, tags=%s, timestamp=%d}",
				name, value, type, tags, timestamp);
	}
}
