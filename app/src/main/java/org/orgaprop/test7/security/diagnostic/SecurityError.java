package org.orgaprop.test7.security.diagnostic;

import lombok.Value;
import lombok.Builder;
import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

@Value
public class SecurityError {

	private static final int MAX_STACK_DEPTH = 50;

	@Nonnull
	SecurityCategory category;
	@Nonnull
	SecuritySeverity severity;
	@Nonnull
	String message;
	@Nonnull
	LocalDateTime timestamp;
	@Nonnull
	String stackTrace;
	@Nonnull
	String threadName;
	@Nonnull
	Map<String, String> metadata = new ConcurrentHashMap<>();
	private final Exception originalException;

	private static final ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

	public SecurityError(SecurityCategory category, Exception e) {
		if (category == null || e == null) {
			throw new IllegalArgumentException("La catégorie et l'exception ne peuvent pas être null");
		}
		this.category = category;
		this.severity = determineSeverity(e);
		this.message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
		this.timestamp = LocalDateTime.now();
		this.stackTrace = truncateStackTrace(ExceptionUtils.getStackTrace(e));
		this.threadName = Thread.currentThread().getName();
		this.metadata = new ConcurrentHashMap<>();
		this.originalException = e;
	}

	public SecurityError(SecurityCategory category, SecuritySeverity severity,
			String message, LocalDateTime timestamp, String stackTrace,
			String threadName, Map<String, String> metadata) {
		validateParameters(category, severity, message, timestamp, stackTrace, threadName);
		this.category = category;
		this.severity = severity;
		this.message = message;
		this.timestamp = timestamp;
		this.stackTrace = stackTrace;
		this.threadName = threadName;
		this.metadata = new ConcurrentHashMap<>(metadata);
		this.originalException = null;
	}

	private SecuritySeverity determineSeverity(Exception e) {
		if (e instanceof SecurityException || e instanceof RuntimeException) {
			return SecuritySeverity.CRITICAL;
		}
		return SecuritySeverity.WARNING;
	}

	private String truncateStackTrace(String fullTrace) {
		String[] lines = fullTrace.split("\n");
		int length = Math.min(lines.length, MAX_STACK_DEPTH);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(lines[i]).append("\n");
		}
		return sb.toString();
	}

	public boolean isCritical() {
		return severity == SecuritySeverity.CRITICAL;
	}

	public Duration getAge() {
		return Duration.between(timestamp, LocalDateTime.now());
	}

	@Override
	public String toString() {
		return String.format("[%s] %s - %s (%s)",
				severity, category.getLabel(), message, timestamp);
	}

	@JsonSerialize
	public String toJson() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new SecurityException("Erreur de sérialisation JSON", e);
		}
	}

	public static SecurityError fromJson(String json) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(json, SecurityError.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Erreur de désérialisation", e);
		}
	}

	@Builder
	public static class SecurityErrorBuilder {
		// Le builder actuel ne gère pas tous les champs
		private SecurityCategory category;
		private SecuritySeverity severity;
		private String message;
		private LocalDateTime timestamp;
		private String stackTrace;
		private String threadName;
		private Map<String, String> metadata;
		private Exception exception;

		public SecurityError build() {
			if (timestamp == null) {
				timestamp = LocalDateTime.now();
			}
			if (threadName == null) {
				threadName = Thread.currentThread().getName();
			}
			validateFields();
			return new SecurityError(category, severity, message,
					timestamp, stackTrace, threadName,
					metadata != null ? metadata : new ConcurrentHashMap<>());
		}

		private void validateFields() {
			List<String> errors = new ArrayList<>();
			if (category == null)
				errors.add("category est obligatoire");
			if (severity == null)
				errors.add("severity est obligatoire");
			if (message == null)
				errors.add("message est obligatoire");
			if (timestamp == null)
				errors.add("timestamp est obligatoire");
			if (stackTrace == null)
				errors.add("stackTrace est obligatoire");
			if (threadName == null)
				errors.add("threadName est obligatoire");

			if (!errors.isEmpty()) {
				throw new IllegalArgumentException(
						"Erreurs de validation : " + String.join(", ", errors));
			}
		}
	}

	public SecurityError withMetadata(String key, String value) {
		Map<String, String> newMetadata = new ConcurrentHashMap<>(this.metadata);
		newMetadata.put(key, value);
		return new SecurityError(category, severity, message, timestamp, stackTrace, threadName, newMetadata);
	}

	public boolean isSimilarTo(SecurityError other, double threshold) {
		return this.category == other.category &&
				this.severity == other.severity &&
				calculateSimilarity(this.message, other.message) > threshold;
	}

	private double calculateSimilarity(String s1, String s2) {
		if (s1 == null || s2 == null)
			return 0.0;
		int maxLength = Math.max(s1.length(), s2.length());
		if (maxLength == 0)
			return 1.0;
		return 1.0 - ((double) StringUtils.getLevenshteinDistance(s1, s2)) / maxLength;
	}

	public Map<String, Object> getMetrics() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("severity_level", severity.ordinal());
		metrics.put("age_seconds", getAge().getSeconds());
		metrics.put("stack_depth", stackTrace.split("\n").length);
		return metrics;
	}

	public List<SecurityError> getNestedErrors() {
		List<SecurityError> nested = new ArrayList<>();
		Throwable cause = this.getOriginalException().getCause();
		while (cause != null && cause instanceof Exception) {
			nested.add(new SecurityError(this.category, (Exception) cause));
			cause = cause.getCause();
		}
		return nested;
	}

	public Exception getOriginalException() {
		return originalException;
	}

	// Les deux constructeurs ont des validations différentes
	// À uniformiser dans une méthode commune :
	private void validateParameters(SecurityCategory category, SecuritySeverity severity,
			String message, LocalDateTime timestamp,
			String stackTrace, String threadName) {
		Map<String, String> validations = new HashMap<>();
		validations.put("category", category != null ? null : "ne peut pas être null");
		validations.put("severity", severity != null ? null : "ne peut pas être null");
		validations.put("message", StringUtils.isNotBlank(message) ? null : "ne peut pas être vide");
		validations.put("timestamp", timestamp != null ? null : "ne peut pas être null");
		validations.put("stackTrace", StringUtils.isNotBlank(stackTrace) ? null : "ne peut pas être vide");
		validations.put("threadName", StringUtils.isNotBlank(threadName) ? null : "ne peut pas être vide");

		List<String> errors = validations.entrySet().stream()
				.filter(e -> e.getValue() != null)
				.map(e -> e.getKey() + " " + e.getValue())
				.collect(Collectors.toList());

		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(
					"Paramètres invalides : " + String.join(", ", errors));
		}
	}

	@VisibleForTesting
	public static SecurityError createForTesting(SecurityCategory category,
			String message,
			SecuritySeverity severity) {
		return new SecurityError(category,
				new SecurityException(message),
				severity);
	}

	public enum ErrorCategory {
		SYSTEM, SECURITY, RESOURCE, VALIDATION;
	}

	public ErrorCategory categorize() {
		if (message.contains("mémoire") || message.contains("ressource")) {
			return ErrorCategory.RESOURCE;
		}
		if (message.contains("sécurité") || message.contains("accès")) {
			return ErrorCategory.SECURITY;
		}
		return ErrorCategory.SYSTEM;
	}

	public boolean isInTimeWindow(Duration window) {
		return getAge().compareTo(window) <= 0;
	}

	public Map<String, Object> getDebugInfo() {
		Map<String, Object> debug = new HashMap<>(getMetrics());
		debug.put("thread_info", Thread.currentThread().getName() + "/" + Thread.currentThread().getId());
		debug.put("creation_time", timestamp);
		debug.put("stack_summary", getStackTraceSummary());
		return debug;
	}

	private String getStackTraceSummary() {
		return Arrays.stream(stackTrace.split("\n"))
				.limit(3)
				.collect(Collectors.joining("\n"));
	}

	private void validateCategory(SecurityCategory category) {
		if (category == null) {
			throw new IllegalArgumentException("La catégorie ne peut pas être null");
		}
		// Autres validations spécifiques à la catégorie
	}

}
