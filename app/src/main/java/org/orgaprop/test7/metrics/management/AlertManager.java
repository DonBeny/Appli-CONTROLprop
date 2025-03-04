package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.time.LocalDateTime;

public class AlertManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(AlertSystem.class);

	private static final int MAX_ALERTS_HISTORY = 1000;
	private static final int ALERT_QUEUE_CAPACITY = 500;
	private static final long ALERT_PROCESSING_TIMEOUT = 5000; // 5 secondes

	private final BlockingQueue<Alert> alertQueue;
	private final ConcurrentNavigableMap<Long, Alert> alertHistory;
	private final ExecutorService alertProcessor;
	private final AtomicBoolean isRunning;
	private final AlertMetrics metrics;
	private final AlertThrottler throttler;
	private final AlertPrioritizer prioritizer;

	public AlertSystem() {
        this.alertQueue = new LinkedBlockingQueue<>(ALERT_QUEUE_CAPACITY);
        this.alertHistory = new ConcurrentSkipListMap<>();
        this.alertProcessor = createAlertProcessor();
        this.isRunning = new AtomicBoolean(true);
        this.metrics = new AlertMetrics();
        this.throttler = new AlertThrottler();
        this.prioritizer = new AlertPrioritizer();
        
        startAlertProcessing();
    }

	public void raiseAlert(String type, Map<String, Object> data) {
		if (!isRunning.get()) {
			logger.warn("Tentative de levée d'alerte alors que le système est arrêté");
			return;
		}

		try {
			Alert alert = new Alert(type, enrichAlertData(data));
			if (throttler.shouldProcess(alert)) {
				processAlert(alert);
			} else {
				metrics.recordThrottled(type);
			}
		} catch (Exception e) {
			handleAlertError(e, type, data);
		}
	}

	private void processAlert(Alert alert) {
		if (!alertQueue.offer(alert)) {
			handleQueueFull(alert);
			return;
		}
		metrics.recordQueued(alert.type);
	}

	private void startAlertProcessing() {
		alertProcessor.submit(() -> {
			while (isRunning.get() || !alertQueue.isEmpty()) {
				try {
					Alert alert = alertQueue.poll(ALERT_PROCESSING_TIMEOUT, TimeUnit.MILLISECONDS);
					if (alert != null) {
						handleAlert(alert);
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					logger.error("Erreur lors du traitement des alertes", e);
				}
			}
		});
	}

	private void handleAlert(Alert alert) {
		try {
			alert.priority = prioritizer.calculatePriority(alert);
			addToHistory(alert);
			notifyAlert(alert);
			metrics.recordProcessed(alert.type);
		} catch (Exception e) {
			metrics.recordError(alert.type);
			logger.error("Erreur lors du traitement de l'alerte: {}", alert.type, e);
		}
	}

	private void addToHistory(Alert alert) {
		alertHistory.put(alert.timestamp, alert);
		while (alertHistory.size() > MAX_ALERTS_HISTORY) {
			alertHistory.pollFirstEntry();
		}
	}

	private Map<String, Object> enrichAlertData(Map<String, Object> data) {
		Map<String, Object> enrichedData = new HashMap<>(data);
		enrichedData.put("timestamp", System.currentTimeMillis());
		enrichedData.put("thread", Thread.currentThread().getName());
		return enrichedData;
	}

	private void notifyAlert(Alert alert) {
		switch (alert.priority) {
			case HIGH:
				logger.error("ALERTE CRITIQUE: {} - {}", alert.type, alert.data);
				break;
			case MEDIUM:
				logger.warn("Alerte: {} - {}", alert.type, alert.data);
				break;
			case LOW:
				logger.info("Notification: {} - {}", alert.type, alert.data);
				break;
		}
	}

	public Map<String, Object> getAlertStats() {
		return metrics.getStats();
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			alertProcessor.shutdown();
			try {
				if (!alertProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
					alertProcessor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				alertProcessor.shutdownNow();
			}
			metrics.reset();
		}
	}

	private static class Alert {
		final String type;
		final Map<String, Object> data;
		final long timestamp;
		AlertPriority priority;

		Alert(String type, Map<String, Object> data) {
			this.type = type;
			this.data = new HashMap<>(data);
			this.timestamp = System.currentTimeMillis();
		}
	}

	private enum AlertPriority {
		LOW, MEDIUM, HIGH
	}

	private static class AlertMetrics {
		private final Map<String, AtomicInteger> queuedByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> processedByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> throttledByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorsByType = new ConcurrentHashMap<>();

		void recordQueued(String type) {
			incrementCounter(queuedByType, type);
		}

		void recordProcessed(String type) {
			incrementCounter(processedByType, type);
		}

		void recordThrottled(String type) {
			incrementCounter(throttledByType, type);
		}

		void recordError(String type) {
			incrementCounter(errorsByType, type);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String type) {
			counters.computeIfAbsent(type, k -> new AtomicInteger()).incrementAndGet();
		}

		Map<String, Object> getStats() {
			Map<String, Object> stats = new HashMap<>();
			stats.put("queued", new HashMap<>(queuedByType));
			stats.put("processed", new HashMap<>(processedByType));
			stats.put("throttled", new HashMap<>(throttledByType));
			stats.put("errors", new HashMap<>(errorsByType));
			return stats;
		}

		void reset() {
			queuedByType.clear();
			processedByType.clear();
			throttledByType.clear();
			errorsByType.clear();
		}
	}
}
