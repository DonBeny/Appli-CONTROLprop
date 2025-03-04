package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class NotificationManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(NotificationManager.class);

	private static final int MAX_NOTIFICATIONS = 100;
	private static final long NOTIFICATION_TIMEOUT = 10_000; // 10 secondes
	private static final int BATCH_SIZE = 10;

	private final BlockingQueue<Notification> notificationQueue;
	private final Map<String, NotificationSubscriber> subscribers;
	private final ScheduledExecutorService scheduler;
	private final NotificationMetrics metrics;
	private final AtomicBoolean isRunning;
	private final AlertManager alertManager;

	public NotificationManager(AlertManager alertManager) {
		this.notificationQueue = new LinkedBlockingQueue<>(MAX_NOTIFICATIONS);
		this.subscribers = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.metrics = new NotificationMetrics();
		this.isRunning = new AtomicBoolean(true);
		this.alertManager = alertManager;

		startProcessing();
	}

	private void startProcessing() {
		scheduler.scheduleWithFixedDelay(
				this::processBatch,
				0,
				1000,
				TimeUnit.MILLISECONDS);
	}

	public void subscribe(String type, NotificationSubscriber subscriber) {
		subscribers.put(type, subscriber);
		metrics.recordSubscription(type);
	}

	public void unsubscribe(String type) {
		subscribers.remove(type);
		metrics.recordUnsubscription(type);
	}

	public void notify(String type, Map<String, Object> data) {
		if (!isRunning.get()) {
			logger.warn("Tentative de notification alors que le manager est arrêté");
			return;
		}

		try {
			Notification notification = new Notification(type, data);
			if (!notificationQueue.offer(notification)) {
				handleQueueFull(notification);
				return;
			}
			metrics.recordQueued(type);
		} catch (Exception e) {
			handleNotificationError(e, type, data);
		}
	}

	private void processBatch() {
		List<Notification> batch = new ArrayList<>(BATCH_SIZE);
		notificationQueue.drainTo(batch, BATCH_SIZE);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processBatchNotifications(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	private void processBatchNotifications(List<Notification> batch) {
		Map<String, List<Notification>> notificationsByType = batch.stream()
				.collect(groupingBy(n -> n.type));

		notificationsByType.forEach((type, notifications) -> {
			NotificationSubscriber subscriber = subscribers.get(type);
			if (subscriber != null) {
				try {
					subscriber.onNotifications(notifications);
					metrics.recordDelivered(type, notifications.size());
				} catch (Exception e) {
					handleDeliveryError(e, type, notifications.size());
				}
			}
		});
	}

	private void handleQueueFull(Notification notification) {
		metrics.recordDropped(notification.type);
		alertManager.raiseAlert("NOTIFICATION_QUEUE_FULL", Map.of(
				"type", notification.type,
				"queueSize", notificationQueue.size()));
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
			cleanup();
		}
	}

	private static class NotificationMetrics {
		private final Map<String, AtomicInteger> queuedByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> deliveredByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> droppedByType = new ConcurrentHashMap<>();
		private final AtomicLong totalProcessingTime = new AtomicLong(0);
		private final AtomicInteger batchesProcessed = new AtomicInteger(0);

		void recordQueued(String type) {
			incrementCounter(queuedByType, type);
		}

		void recordDelivered(String type, int count) {
			deliveredByType.computeIfAbsent(type, k -> new AtomicInteger())
					.addAndGet(count);
		}

		void recordDropped(String type) {
			incrementCounter(droppedByType, type);
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
					"queued", new HashMap<>(queuedByType),
					"delivered", new HashMap<>(deliveredByType),
					"dropped", new HashMap<>(droppedByType),
					"averageProcessingTime", getAverageProcessingTime(),
					"batchesProcessed", batchesProcessed.get());
		}

		private double getAverageProcessingTime() {
			int batches = batchesProcessed.get();
			return batches > 0 ? (double) totalProcessingTime.get() / batches : 0;
		}
	}
}
