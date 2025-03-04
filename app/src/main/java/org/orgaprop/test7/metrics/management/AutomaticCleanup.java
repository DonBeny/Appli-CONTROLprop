package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;

public class AutomaticCleanup implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(AutomaticCleanup.class);

	private static final long DEFAULT_CLEANUP_INTERVAL = 5 * 60 * 1000; // 5 minutes
	private static final int MAX_CLEANUP_ATTEMPTS = 3;
	private static final long EMERGENCY_CLEANUP_TIMEOUT = 30_000; // 30 secondes

	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final AtomicBoolean isPaused;
	private final AtomicInteger cleanupCounter;
	private final CleanupStrategy cleanupStrategy;
	private final CleanupMetrics metrics;
	private ScheduledFuture<?> cleanupTask;

	public AutomaticCleanup(CleanupStrategy strategy) {
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "cleanup-worker");
			t.setDaemon(true);
			return t;
		});
		this.isRunning = new AtomicBoolean(false);
		this.isPaused = new AtomicBoolean(false);
		this.cleanupCounter = new AtomicInteger(0);
		this.cleanupStrategy = strategy;
		this.metrics = new CleanupMetrics();
	}

	public void startPeriodicCleanup() {
		startPeriodicCleanup(DEFAULT_CLEANUP_INTERVAL);
	}

	public void startPeriodicCleanup(long interval) {
		if (cleanupTask != null && !cleanupTask.isDone()) {
			logger.warn("Tâche de nettoyage déjà en cours");
			return;
		}

		cleanupTask = scheduler.scheduleAtFixedRate(
				() -> performCleanupSafely(),
				interval,
				interval,
				TimeUnit.MILLISECONDS);

		logger.info("Nettoyage automatique démarré avec intervalle de {} ms", interval);
	}

	private void performCleanupSafely() {
		if (isPaused.get() || !isRunning.compareAndSet(false, true)) {
			return;
		}

		try {
			long startTime = System.nanoTime();
			int itemsCleaned = cleanupStrategy.cleanup();
			long duration = (System.nanoTime() - startTime) / 1_000_000; // en ms

			metrics.recordCleanup(itemsCleaned, duration);
			cleanupCounter.incrementAndGet();

			logger.debug("Nettoyage effectué : {} éléments en {} ms", itemsCleaned, duration);
		} catch (Exception e) {
			logger.error("Erreur pendant le nettoyage automatique", e);
			metrics.recordFailure();
			handleCleanupError(e);
		} finally {
			isRunning.set(false);
		}
	}

	public void performEmergencyCleanup() {
		if (!isRunning.compareAndSet(false, true)) {
			logger.warn("Nettoyage d'urgence impossible : déjà en cours");
			return;
		}

		try {
			logger.info("Début du nettoyage d'urgence");
			int itemsCleaned = cleanupStrategy.emergencyCleanup();
			metrics.recordEmergencyCleanup(itemsCleaned);
		} catch (Exception e) {
			logger.error("Erreur pendant le nettoyage d'urgence", e);
			metrics.recordFailure();
		} finally {
			isRunning.set(false);
		}
	}

	public void pause() {
		isPaused.set(true);
		logger.info("Nettoyage automatique mis en pause");
	}

	public void resume() {
		isPaused.set(false);
		logger.info("Nettoyage automatique repris");
	}

	public Map<String, Object> getMetrics() {
		return metrics.getStats();
	}

	@Override
	public void close() {
		try {
			if (cleanupTask != null) {
				cleanupTask.cancel(false);
			}
			scheduler.shutdown();
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			scheduler.shutdownNow();
		} finally {
			logger.info("AutomaticCleanup arrêté");
		}
	}

	private static class CleanupMetrics {
		private final AtomicInteger totalCleanups = new AtomicInteger(0);
		private final AtomicInteger totalItemsCleaned = new AtomicInteger(0);
		private final AtomicInteger failureCount = new AtomicInteger(0);
		private final AtomicLong totalDuration = new AtomicLong(0);

		void recordCleanup(int itemsCleaned, long duration) {
			totalCleanups.incrementAndGet();
			totalItemsCleaned.addAndGet(itemsCleaned);
			totalDuration.addAndGet(duration);
		}

		void recordEmergencyCleanup(int itemsCleaned) {
			totalCleanups.incrementAndGet();
			totalItemsCleaned.addAndGet(itemsCleaned);
		}

		void recordFailure() {
			failureCount.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalCleanups", totalCleanups.get(),
					"totalItemsCleaned", totalItemsCleaned.get(),
					"failureCount", failureCount.get(),
					"averageDuration", getAverageDuration());
		}

		private double getAverageDuration() {
			int cleanups = totalCleanups.get();
			return cleanups > 0 ? (double) totalDuration.get() / cleanups : 0;
		}
	}
}
