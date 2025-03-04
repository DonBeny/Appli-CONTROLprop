package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class OperationsManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(OperationsManager.class);

	private static final int DEFAULT_QUEUE_CAPACITY = 1000;
	private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private static final long MAX_OPERATION_TIME = 30_000; // 30 secondes

	private final BlockingQueue<MetricsOperation> operationQueue;
	private final ExecutorService executor;
	private final Map<String, OperationStats> operationStats;
	private final AtomicBoolean isRunning;
	private final AlertSystem alertSystem;
	private final RetryPolicy retryPolicy;

	public OperationsManager(AlertSystem alertSystem) {
		this.operationQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
		this.executor = createExecutor();
		this.operationStats = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.alertSystem = alertSystem;
		this.retryPolicy = new RetryPolicy();

		startProcessing();
	}

	public void submitOperation(String operationType, Runnable task, OperationPriority priority) {
		if (!isRunning.get()) {
			throw new IllegalStateException("OperationsManager est arrêté");
		}

		MetricsOperation operation = new MetricsOperation(operationType, task, priority);
		if (!operationQueue.offer(operation)) {
			handleQueueFull(operation);
			return;
		}

		operationStats.computeIfAbsent(operationType, k -> new OperationStats())
				.recordSubmission();
	}

	private void startProcessing() {
		for (int i = 0; i < DEFAULT_THREAD_POOL_SIZE; i++) {
			executor.submit(this::processOperations);
		}
	}

	private void processOperations() {
		while (isRunning.get()) {
			try {
				MetricsOperation operation = operationQueue.poll(1, TimeUnit.SECONDS);
				if (operation != null) {
					processOperation(operation);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void processOperation(MetricsOperation operation) {
		OperationStats stats = operationStats.get(operation.type);
		long startTime = System.nanoTime();

		try {
			executeWithRetry(operation);
			recordSuccess(operation, startTime);
		} catch (Exception e) {
			handleOperationError(operation, e);
		}
	}

	private void executeWithRetry(MetricsOperation operation) throws Exception {
		int attempts = 0;
		Exception lastException = null;

		while (attempts < retryPolicy.getMaxAttempts()) {
			try {
				operation.task.run();
				return;
			} catch (Exception e) {
				lastException = e;
				attempts++;
				if (!retryPolicy.shouldRetry(operation.type, attempts)) {
					break;
				}
				Thread.sleep(retryPolicy.getDelayForAttempt(attempts));
			}
		}

		throw new OperationFailedException("Échec après " + attempts + " tentatives", lastException);
	}

	private void recordSuccess(MetricsOperation operation, long startTime) {
		long duration = System.nanoTime() - startTime;
		OperationStats stats = operationStats.get(operation.type);
		if (stats != null) {
			stats.recordSuccess(duration);
		}
	}

	private void handleOperationError(MetricsOperation operation, Exception e) {
		logger.error("Erreur lors de l'exécution de l'opération {}: {}",
				operation.type, e.getMessage());

		OperationStats stats = operationStats.get(operation.type);
		if (stats != null) {
			stats.recordFailure();
		}

		alertSystem.raiseAlert("OPERATION_FAILURE", Map.of(
				"type", operation.type,
				"error", e.getMessage(),
				"stats", stats != null ? stats.getStats() : Map.of()));
	}

	private ExecutorService createExecutor() {
		return new ThreadPoolExecutor(
				DEFAULT_THREAD_POOL_SIZE,
				DEFAULT_THREAD_POOL_SIZE,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(),
				new ThreadFactory() {
					private final AtomicInteger count = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r, "operations-worker-" + count.incrementAndGet());
						t.setDaemon(true);
						return t;
					}
				});
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

	private static class OperationStats {
		private final AtomicLong submissions = new AtomicLong(0);
		private final AtomicLong successes = new AtomicLong(0);
		private final AtomicLong failures = new AtomicLong(0);
		private final AtomicLong totalDuration = new AtomicLong(0);

		void recordSubmission() {
			submissions.incrementAndGet();
		}

		void recordSuccess(long duration) {
			successes.incrementAndGet();
			totalDuration.addAndGet(duration);
		}

		void recordFailure() {
			failures.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"submissions", submissions.get(),
					"successes", successes.get(),
					"failures", failures.get(),
					"averageDuration", getAverageDuration());
		}

		private double getAverageDuration() {
			long total = successes.get();
			return total > 0 ? (double) totalDuration.get() / total : 0;
		}
	}
}
