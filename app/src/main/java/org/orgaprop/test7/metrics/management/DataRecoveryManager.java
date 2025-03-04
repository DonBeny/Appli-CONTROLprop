package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.Queue;

public class DataRecoveryManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(DataRecoveryManager.class);

	private static final int MAX_RECOVERY_ATTEMPTS = 3;
	private static final long RECOVERY_TIMEOUT = 30_000; // 30 secondes
	private static final long COOLDOWN_PERIOD = 5_000; // 5 secondes

	private final Queue<RecoveryTask> recoveryQueue;
	private final AtomicBoolean isRecovering;
	private final AtomicInteger recoveryAttempts;
	private final ScheduledExecutorService scheduler;
	private final RecoveryMetrics metrics;
	private final AlertSystem alertSystem;

	public DataRecoveryManager(AlertSystem alertSystem) {
		this.recoveryQueue = new ConcurrentLinkedQueue<>();
		this.isRecovering = new AtomicBoolean(false);
		this.recoveryAttempts = new AtomicInteger(0);
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.metrics = new RecoveryMetrics();
		this.alertSystem = alertSystem;
	}

	public void initiateRecovery(String component, Map<String, Object> context) {
		if (isRecovering.compareAndSet(false, true)) {
			try {
				RecoveryTask task = new RecoveryTask(component, context);
				recoveryQueue.offer(task);
				processRecoveryQueue();
			} catch (Exception e) {
				handleRecoveryError(e, component);
			}
		} else {
			logger.warn("Récupération déjà en cours pour {}", component);
		}
	}

	private void processRecoveryQueue() {
		while (!recoveryQueue.isEmpty() && recoveryAttempts.get() < MAX_RECOVERY_ATTEMPTS) {
			RecoveryTask task = recoveryQueue.poll();
			if (task != null) {
				performRecovery(task);
			}
		}

		if (!recoveryQueue.isEmpty()) {
			handleFailedRecovery();
		}
	}

	private void performRecovery(RecoveryTask task) {
		try {
			logger.info("Début récupération pour {}", task.component);
			long startTime = System.nanoTime();

			// Exécution de la récupération
			boolean success = executeRecoveryStrategy(task);

			if (success) {
				handleSuccessfulRecovery(task, startTime);
			} else {
				handleFailedTaskRecovery(task);
			}
		} catch (Exception e) {
			handleRecoveryError(e, task.component);
		}
	}

	private boolean executeRecoveryStrategy(RecoveryTask task) {
		// Implémentation des stratégies de récupération selon le composant
		switch (task.component) {
			case "memory":
				return performMemoryRecovery(task);
			case "metrics":
				return performMetricsRecovery(task);
			default:
				return performDefaultRecovery(task);
		}
	}

	private void handleSuccessfulRecovery(RecoveryTask task, long startTime) {
		long duration = (System.nanoTime() - startTime) / 1_000_000; // en ms
		metrics.recordSuccess(duration);
		recoveryAttempts.set(0);
		isRecovering.set(false);

		logger.info("Récupération réussie pour {} en {} ms", task.component, duration);
	}

	private void handleFailedTaskRecovery(RecoveryTask task) {
		int attempts = recoveryAttempts.incrementAndGet();
		logger.warn("Échec récupération pour {} (tentative {})", task.component, attempts);

		if (attempts < MAX_RECOVERY_ATTEMPTS) {
			scheduleRetry(task);
		}
	}

	private void scheduleRetry(RecoveryTask task) {
		scheduler.schedule(() -> {
			recoveryQueue.offer(task);
			processRecoveryQueue();
		}, COOLDOWN_PERIOD, TimeUnit.MILLISECONDS);
	}

	private void handleFailedRecovery() {
		isRecovering.set(false);
		metrics.recordFailure();

		Map<String, Object> alertData = Map.of(
				"queueSize", recoveryQueue.size(),
				"attempts", recoveryAttempts.get(),
				"metrics", metrics.getStats());

		alertSystem.raiseAlert("RECOVERY_FAILED", alertData);
	}

	private void handleRecoveryError(Exception e, String component) {
		logger.error("Erreur lors de la récupération de {}: {}", component, e.getMessage(), e);
		metrics.recordError();

		Map<String, Object> errorData = Map.of(
				"component", component,
				"error", e.getMessage(),
				"attempts", recoveryAttempts.get());

		alertSystem.raiseAlert("RECOVERY_ERROR", errorData);
	}

	@Override
	public void close() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			scheduler.shutdownNow();
		}
	}

	private static class RecoveryTask {
		final String component;
		final Map<String, Object> context;

		RecoveryTask(String component, Map<String, Object> context) {
			this.component = component;
			this.context = context;
		}
	}

	private static class RecoveryMetrics {
		private final AtomicInteger successCount = new AtomicInteger(0);
		private final AtomicInteger failureCount = new AtomicInteger(0);
		private final AtomicInteger errorCount = new AtomicInteger(0);
		private final AtomicLong totalDuration = new AtomicLong(0);

		void recordSuccess(long duration) {
			successCount.incrementAndGet();
			totalDuration.addAndGet(duration);
		}

		void recordFailure() {
			failureCount.incrementAndGet();
		}

		void recordError() {
			errorCount.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"successes", successCount.get(),
					"failures", failureCount.get(),
					"errors", errorCount.get(),
					"averageDuration", getAverageDuration());
		}

		private double getAverageDuration() {
			int successes = successCount.get();
			return successes > 0 ? (double) totalDuration.get() / successes : 0;
		}
	}
}
