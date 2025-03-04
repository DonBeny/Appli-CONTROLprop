package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ErrorManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ErrorManager.class);

	private static final int MAX_ERROR_HISTORY = 1000;
	private static final long ERROR_CHECK_INTERVAL = 60_000; // 1 minute
	private static final int ERROR_THRESHOLD = 10; // Seuil d'erreurs par minute

	private final ConcurrentNavigableMap<Long, ErrorEntry> errorHistory;
	private final Map<String, ErrorStats> errorStatsByType;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final ErrorMetrics metrics;
	private final AlertManager alertManager;

	public ErrorManager(AlertManager alertManager) {
		this.errorHistory = new ConcurrentSkipListMap<>();
		this.errorStatsByType = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ErrorMetrics();
		this.alertManager = alertManager;

		startErrorMonitoring();
	}

	public void recordError(String type, Throwable error, Map<String, Object> context) {
		if (!isRunning.get()) {
			logger.warn("Tentative d'enregistrement d'erreur alors que le manager est arrêté");
			return;
		}

		try {
			ErrorEntry entry = new ErrorEntry(type, error, context);
			updateErrorStats(entry);
			addToHistory(entry);
			metrics.recordError(type);

			if (shouldTriggerAlert(type)) {
				handleErrorThreshold(type);
			}
		} catch (Exception e) {
			logger.error("Erreur lors de l'enregistrement de l'erreur", e);
		}
	}

	private void startErrorMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::analyzeErrors,
				ERROR_CHECK_INTERVAL,
				ERROR_CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void updateErrorStats(ErrorEntry entry) {
		ErrorStats stats = errorStatsByType.computeIfAbsent(
				entry.type,
				k -> new ErrorStats());
		stats.recordError(entry);
	}

	private void addToHistory(ErrorEntry entry) {
		errorHistory.put(entry.timestamp, entry);
		while (errorHistory.size() > MAX_ERROR_HISTORY) {
			errorHistory.pollFirstEntry();
		}
	}

	private boolean shouldTriggerAlert(String type) {
		ErrorStats stats = errorStatsByType.get(type);
		return stats != null && stats.getRecentErrorCount() > ERROR_THRESHOLD;
	}

	private void analyzeErrors() {
		try {
			cleanupOldErrors();
			detectErrorPatterns();
			updateMetrics();
		} catch (Exception e) {
			logger.error("Erreur lors de l'analyse des erreurs", e);
		}
	}

	private void cleanupOldErrors() {
		long cutoff = System.currentTimeMillis() - ERROR_CHECK_INTERVAL;
		errorHistory.headMap(cutoff).clear();
	}

	private void detectErrorPatterns() {
		errorStatsByType.forEach((type, stats) -> {
			if (stats.hasErrorSpike()) {
				handleErrorSpike(type, stats);
			}
		});
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
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
	}

	private static class ErrorEntry {
		final String type;
		final Throwable error;
		final Map<String, Object> context;
		final long timestamp;

		ErrorEntry(String type, Throwable error, Map<String, Object> context) {
			this.type = type;
			this.error = error;
			this.context = new HashMap<>(context);
			this.timestamp = System.currentTimeMillis();
		}
	}

	private static class ErrorStats {
		private final AtomicInteger totalErrors = new AtomicInteger(0);
		private final Queue<Long> errorTimestamps = new ConcurrentLinkedQueue<>();
		private final Map<String, AtomicInteger> errorsByStack = new ConcurrentHashMap<>();

		void recordError(ErrorEntry entry) {
			totalErrors.incrementAndGet();
			errorTimestamps.offer(entry.timestamp);
			String stackTrace = getStackTraceKey(entry.error);
			errorsByStack.computeIfAbsent(stackTrace, k -> new AtomicInteger())
					.incrementAndGet();

			// Nettoyage des timestamps anciens
			long cutoff = System.currentTimeMillis() - ERROR_CHECK_INTERVAL;
			while (!errorTimestamps.isEmpty() && errorTimestamps.peek() < cutoff) {
				errorTimestamps.poll();
			}
		}

		int getRecentErrorCount() {
			return errorTimestamps.size();
		}

		boolean hasErrorSpike() {
			return getRecentErrorCount() > ERROR_THRESHOLD;
		}

		private String getStackTraceKey(Throwable error) {
			StackTraceElement[] stack = error.getStackTrace();
			if (stack.length > 0) {
				return stack[0].toString();
			}
			return "unknown";
		}
	}

	private static class ErrorMetrics {
		private final Map<String, AtomicInteger> errorsByType = new ConcurrentHashMap<>();
		private final AtomicLong totalErrors = new AtomicLong(0);
		private final AtomicInteger errorSpikes = new AtomicInteger(0);
		private final AtomicLong lastSpikeTime = new AtomicLong(0);

		void recordError(String type) {
			errorsByType.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
			totalErrors.incrementAndGet();
		}

		void recordErrorSpike() {
			errorSpikes.incrementAndGet();
			lastSpikeTime.set(System.currentTimeMillis());
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalErrors", totalErrors.get(),
					"errorsByType", new HashMap<>(errorsByType),
					"errorSpikes", errorSpikes.get(),
					"lastSpikeTime", lastSpikeTime.get());
		}
	}
}
