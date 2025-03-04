package org.orgaprop.test7.metrics.normalization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MetricsNormalizer implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsNormalizer.class);

	private final Map<String, NormalizationRule> rules;
	private final Map<String, TypeConverter<?>> converters;
	private final BlockingQueue<NormalizationRequest> requestQueue;
	private final ExecutorService executor;
	private final AtomicBoolean isRunning;
	private final NormalizationMetrics metrics;
	private final ValidationManager validationManager;
	private final MetricsConfig config;
	private final ConfigModule normalizationConfig;

	public MetricsNormalizer(ValidationManager validationManager) {
		this.config = MetricsConfig.getInstance();
		this.normalizationConfig = config.getModule("normalization");

		if (normalizationConfig == null) {
			logger.error("Configuration 'normalization' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> executionConfig = (Map<String, Object>) normalizationConfig.getProperty("execution");
		int threadPoolSize = (int) executionConfig.get("threadPoolSize");
		int queueCapacity = (int) executionConfig.get("queueCapacity");

		this.rules = new ConcurrentHashMap<>();
		this.converters = new ConcurrentHashMap<>();
		this.requestQueue = new LinkedBlockingQueue<>(queueCapacity);
		this.executor = Executors.newFixedThreadPool(threadPoolSize);
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new NormalizationMetrics();
		this.validationManager = validationManager;

		initializeDefaultRules();
		initializeDefaultConverters();
	}

	private void initializeDefaultRules() {
		Map<String, Object> rulesConfig = (Map<String, Object>) normalizationConfig.getProperty("rules");

		rulesConfig.forEach((key, value) -> {
			Map<String, Object> ruleConfig = (Map<String, Object>) value;
			NormalizationRule rule = new NormalizationRule()
					.setRequired((boolean) ruleConfig.get("required"))
					.setType(getClassForType((String) ruleConfig.get("type")));

			List<String> transformations = (List<String>) ruleConfig.get("transformations");
			for (String transformation : transformations) {
				rule.addTransformation(getTransformationForName(transformation));
			}

			rules.put(key, rule);
		});
	}

	private Class<?> getClassForType(String type) {
		switch (type) {
			case "Long":
				return Long.class;
			case "Double":
				return Double.class;
			default:
				return Object.class;
		}
	}

	public Future<Map<String, Object>> normalize(Map<String, Object> rawMetrics) {
		validateInput(rawMetrics);
		NormalizationRequest request = new NormalizationRequest(rawMetrics);
		requestQueue.offer(request);
		metrics.recordSubmission();
		return request.future;
	}

	private void processRequest(NormalizationRequest request) {
		try {
			Map<String, Object> normalized = new HashMap<>();
			request.metrics.forEach((key, value) -> {
				try {
					normalized.put(key, normalizeValue(key, value));
				} catch (Exception e) {
					handleNormalizationError(key, value, e);
				}
			});

			validateNormalizedMetrics(normalized);
			request.future.complete(normalized);
			metrics.recordSuccess();
		} catch (Exception e) {
			request.future.completeExceptionally(e);
			metrics.recordFailure();
		}
	}

	private Object normalizeValue(String key, Object value) {
		NormalizationRule rule = rules.get(key);
		if (rule != null) {
			value = rule.apply(value);
		}

		TypeConverter<?> converter = converters.get(key);
		if (converter != null) {
			value = converter.convert(value);
		}

		return value;
	}

	private void validateNormalizedMetrics(Map<String, Object> metrics) {
		validationManager.validate(metrics);
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				executor.shutdownNow();
			}
		}
	}

	private static class NormalizationRule {
		private final Class<?> type;
		private final boolean required;
		private final List<Function<Object, Object>> transformations;

		NormalizationRule() {
			this.type = Object.class;
			this.required = false;
			this.transformations = new ArrayList<>();
		}

		NormalizationRule setType(Class<?> type) {
			this.type = type;
			return this;
		}

		NormalizationRule setRequired(boolean required) {
			this.required = required;
			return this;
		}

		NormalizationRule addTransformation(Function<Object, Object> transformation) {
			transformations.add(transformation);
			return this;
		}

		Object apply(Object value) {
			if (value == null && required) {
				throw new IllegalArgumentException("Valeur requise manquante");
			}

			for (Function<Object, Object> transformation : transformations) {
				value = transformation.apply(value);
			}

			return value;
		}
	}

	private static class NormalizationMetrics {
		private final AtomicLong submissionCount = new AtomicLong(0);
		private final AtomicLong successCount = new AtomicLong(0);
		private final AtomicLong failureCount = new AtomicLong(0);
		private final Map<String, AtomicLong> errorsByType = new ConcurrentHashMap<>();

		void recordSubmission() {
			submissionCount.incrementAndGet();
		}

		void recordSuccess() {
			successCount.incrementAndGet();
		}

		void recordFailure() {
			failureCount.incrementAndGet();
		}

		void recordError(String type) {
			errorsByType.computeIfAbsent(type, k -> new AtomicLong())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"submissions", submissionCount.get(),
					"successes", successCount.get(),
					"failures", failureCount.get(),
					"errorsByType", new HashMap<>(errorsByType));
		}
	}
}
