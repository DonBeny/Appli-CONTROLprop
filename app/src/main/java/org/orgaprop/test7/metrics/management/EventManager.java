package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class EventManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(EventManager.class);

	private static final int MAX_QUEUE_SIZE = 10_000;
	private static final int BATCH_SIZE = 100;
	private static final long PROCESSING_INTERVAL = 1000; // 1 seconde

	private final BlockingQueue<MetricEvent> eventQueue;
	private final Map<String, EventHandler> handlers;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final EventMetrics metrics;
	private final AlertManager alertManager;

	public EventManager(AlertManager alertManager) {
		this.eventQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
		this.handlers = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new EventMetrics();
		this.alertManager = alertManager;

		startEventProcessing();
	}

	public void registerHandler(String eventType, EventHandler handler) {
		handlers.put(eventType, handler);
		metrics.recordHandlerRegistration(eventType);
	}

	public void publishEvent(String type, Map<String, Object> data) {
		if (!isRunning.get()) {
			logger.warn("Tentative de publication alors que le manager est arrêté");
			return;
		}

		try {
			MetricEvent event = new MetricEvent(type, enrichEventData(data));
			if (!eventQueue.offer(event)) {
				handleQueueFull(event);
				return;
			}
			metrics.recordEventQueued(type);
		} catch (Exception e) {
			handlePublishError(e, type);
		}
	}

	private void startEventProcessing() {
		scheduler.scheduleWithFixedDelay(
				this::processEventBatch,
				0,
				PROCESSING_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void processEventBatch() {
		List<MetricEvent> batch = new ArrayList<>(BATCH_SIZE);
		eventQueue.drainTo(batch, BATCH_SIZE);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processBatch(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	private void processBatch(List<MetricEvent> batch) {
		Map<String, List<MetricEvent>> eventsByType = batch.stream()
				.collect(groupingBy(MetricEvent::getType));

		eventsByType.forEach(this::processEventType);
	}

	private void processEventType(String type, List<MetricEvent> events) {
		EventHandler handler = handlers.get(type);
		if (handler != null) {
			try {
				handler.handleEvents(events);
				metrics.recordEventsProcessed(type, events.size());
			} catch (Exception e) {
				handleProcessingError(e, type, events.size());
			}
		}
	}

	private Map<String, Object> enrichEventData(Map<String, Object> data) {
		Map<String, Object> enriched = new HashMap<>(data);
		enriched.put("timestamp", System.currentTimeMillis());
		enriched.put("thread", Thread.currentThread().getName());
		return enriched;
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

	public interface EventHandler {
		void handleEvents(List<MetricEvent> events);
	}

	private static class MetricEvent {
		final String type;
		final Map<String, Object> data;
		final long timestamp;

		MetricEvent(String type, Map<String, Object> data) {
			this.type = type;
			this.data = new HashMap<>(data);
			this.timestamp = System.currentTimeMillis();
		}

		String getType() {
			return type;
		}
	}

	private static class EventMetrics {
		private final Map<String, AtomicInteger> queuedByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> processedByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorsByType = new ConcurrentHashMap<>();
		private final AtomicLong totalProcessingTime = new AtomicLong(0);
		private final AtomicInteger batchesProcessed = new AtomicInteger(0);

		void recordHandlerRegistration(String type) {
			// Initialisation des compteurs pour le type
			queuedByType.putIfAbsent(type, new AtomicInteger(0));
			processedByType.putIfAbsent(type, new AtomicInteger(0));
			errorsByType.putIfAbsent(type, new AtomicInteger(0));
		}

		void recordEventQueued(String type) {
			incrementCounter(queuedByType, type);
		}

		void recordEventsProcessed(String type, int count) {
			processedByType.computeIfAbsent(type, k -> new AtomicInteger())
					.addAndGet(count);
		}

		void recordError(String type) {
			incrementCounter(errorsByType, type);
		}

		void recordBatchProcessing(int size, long duration) {
			batchesProcessed.incrementAndGet();
			totalProcessingTime.addAndGet(duration);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String type) {
			counters.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"queuedEvents", new HashMap<>(queuedByType),
					"processedEvents", new HashMap<>(processedByType),
					"errors", new HashMap<>(errorsByType),
					"averageProcessingTime", getAverageProcessingTime(),
					"batchesProcessed", batchesProcessed.get());
		}

		private double getAverageProcessingTime() {
			int batches = batchesProcessed.get();
			return batches > 0 ? (double) totalProcessingTime.get() / batches : 0;
		}
	}
}
