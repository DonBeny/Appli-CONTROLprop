package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ValidationManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ValidationManager.class);

	private static final int MAX_VALIDATION_QUEUE_SIZE = 1000;
	private static final int DEFAULT_VALIDATION_THREADS = Runtime.getRuntime().availableProcessors();
	private static final long VALIDATION_TIMEOUT = 5000; // 5 secondes

	private final BlockingQueue<ValidationTask> validationQueue;
	private final ExecutorService validationExecutor;
	private final Map<String, ValidationRule> validationRules;
	private final AtomicBoolean isRunning;
	private final ValidationMetrics metrics;
	private final AlertManager alertManager;

	public ValidationManager(AlertManager alertManager) {
		this.validationQueue = new LinkedBlockingQueue<>(MAX_VALIDATION_QUEUE_SIZE);
		this.validationExecutor = createValidationExecutor();
		this.validationRules = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ValidationMetrics();
		this.alertManager = alertManager;

		startValidation();
	}

	public void addValidationRule(String type, ValidationRule rule) {
		validationRules.put(type, rule);
	}

	public ValidationResult validate(String type, Object value) {
		if (!isRunning.get()) {
			return ValidationResult.error("ValidationManager est arrêté");
		}

		try {
			ValidationTask task = new ValidationTask(type, value);
			if (!validationQueue.offer(task)) {
				handleQueueFull(task);
				return ValidationResult.error("File de validation pleine");
			}

			return task.getFuture().get(VALIDATION_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			handleValidationError(e, type);
			return ValidationResult.error("Erreur de validation: " + e.getMessage());
		}
	}

	private void startValidation() {
		for (int i = 0; i < DEFAULT_VALIDATION_THREADS; i++) {
			validationExecutor.submit(this::processValidations);
		}
	}

	private void processValidations() {
		while (isRunning.get()) {
			try {
				ValidationTask task = validationQueue.poll(1, TimeUnit.SECONDS);
				if (task != null) {
					processTask(task);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void processTask(ValidationTask task) {
		long startTime = System.nanoTime();
		try {
			ValidationRule rule = validationRules.get(task.type);
			if (rule == null) {
				task.complete(ValidationResult.error("Règle de validation non trouvée"));
				return;
			}

			ValidationResult result = rule.validate(task.value);
			metrics.recordValidation(System.nanoTime() - startTime);
			task.complete(result);

			if (!result.isValid()) {
				handleInvalidData(task.type, task.value, result);
			}
		} catch (Exception e) {
			handleTaskError(e, task);
		}
	}

	private void handleInvalidData(String type, Object value, ValidationResult result) {
		metrics.recordInvalid(type);
		Map<String, Object> alertData = Map.of(
				"type", type,
				"value", value,
				"error", result.getError());
		alertManager.raiseAlert("VALIDATION_FAILURE", alertData);
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			validationExecutor.shutdown();
			try {
				if (!validationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
					validationExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				validationExecutor.shutdownNow();
			}
		}
	}

	private static class ValidationMetrics {
		private final Map<String, AtomicInteger> validationsByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> invalidByType = new ConcurrentHashMap<>();
		private final AtomicLong totalValidationTime = new AtomicLong(0);
		private final AtomicLong validationCount = new AtomicLong(0);

		void recordValidation(long duration) {
			validationCount.incrementAndGet();
			totalValidationTime.addAndGet(duration);
		}

		void recordInvalid(String type) {
			invalidByType.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalValidations", validationCount.get(),
					"averageValidationTime", getAverageValidationTime(),
					"validationsByType", new HashMap<>(validationsByType),
					"invalidByType", new HashMap<>(invalidByType));
		}

		private double getAverageValidationTime() {
			long count = validationCount.get();
			return count > 0 ? (double) totalValidationTime.get() / count : 0;
		}
	}
}
