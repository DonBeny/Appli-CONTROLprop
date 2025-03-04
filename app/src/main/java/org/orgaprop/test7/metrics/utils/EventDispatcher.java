package org.orgaprop.test7.metrics.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class EventDispatcher<T extends Event> implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(EventDispatcher.class);

	private final Map<EventType, Set<EventListener<T>>> listeners;
	private final ExecutorService executor;
	private final AtomicBoolean isRunning;
	private final EventMetrics metrics;
	private final EventFilter<T> filter;
	private final RateLimiter rateLimiter;

	public EventDispatcher() {
		this.listeners = new ConcurrentHashMap<>();
		this.executor = createExecutor();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new EventMetrics();
		this.filter = new EventFilter<>();
		this.rateLimiter = new RateLimiter();
	}

	public void addEventListener(EventType type, EventListener<T> listener) {
		listeners.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet())
				.add(listener);
		metrics.recordListenerAdded(type);
		logger.debug("Listener ajouté pour {}", type);
	}

	public void removeEventListener(EventType type, EventListener<T> listener) {
		Set<EventListener<T>> typeListeners = listeners.get(type);
		if (typeListeners != null) {
			typeListeners.remove(listener);
			metrics.recordListenerRemoved(type);
		}
	}

	public void dispatch(T event) {
		if (!isRunning.get()) {
			logger.warn("Tentative de dispatch d'événement alors que le dispatcher est arrêté");
			return;
		}

		if (!filter.shouldProcess(event) || !rateLimiter.allowEvent()) {
			metrics.recordEventFiltered();
			return;
		}

		Set<EventListener<T>> typeListeners = listeners.get(event.getType());
		if (typeListeners != null && !typeListeners.isEmpty()) {
			dispatchToListeners(event, typeListeners);
		}
	}

	private void dispatchToListeners(T event, Set<EventListener<T>> typeListeners) {
		long startTime = System.nanoTime();

		executor.submit(() -> {
			try {
				notifyListeners(event, typeListeners);
				metrics.recordEventProcessed(System.nanoTime() - startTime);
			} catch (Exception e) {
				handleDispatchError(e, event);
			}
		});
	}

	private void notifyListeners(T event, Set<EventListener<T>> typeListeners) {
		for (EventListener<T> listener : typeListeners) {
			try {
				listener.onEvent(event);
			} catch (Exception e) {
				handleListenerError(e, listener, event);
			}
		}
	}

	private void handleDispatchError(Exception e, T event) {
		logger.error("Erreur lors du dispatch de l'événement {}: {}",
				event.getType(), e.getMessage(), e);
		metrics.recordError();
	}

	private void handleListenerError(Exception e, EventListener<T> listener, T event) {
		logger.error("Erreur du listener {} pour l'événement {}: {}",
				listener.getClass().getSimpleName(),
				event.getType(),
				e.getMessage(), e);
		metrics.recordListenerError();
	}

	private ExecutorService createExecutor() {
		return new ThreadPoolExecutor(
				2, 4, 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(1000),
				new ThreadFactory() {
					private final AtomicInteger count = new AtomicInteger(0);

					@Override
					public Thread newThread(Runnable r) {
						Thread t = new Thread(r, "event-dispatcher-" + count.incrementAndGet());
						t.setDaemon(true);
						return t;
					}
				},
				new ThreadPoolExecutor.CallerRunsPolicy());
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
			listeners.clear();
			metrics.reset();
		}
	}

	private static class EventMetrics {
		private final AtomicLong eventCount = new AtomicLong(0);
		private final AtomicLong filteredCount = new AtomicLong(0);
		private final AtomicLong errorCount = new AtomicLong(0);
		private final AtomicLong listenerErrorCount = new AtomicLong(0);
		private final AtomicLong totalProcessingTime = new AtomicLong(0);
		private final Map<EventType, AtomicInteger> listenersPerType = new ConcurrentHashMap<>();

		void recordEventProcessed(long processingTime) {
			eventCount.incrementAndGet();
			totalProcessingTime.addAndGet(processingTime);
		}

		void recordEventFiltered() {
			filteredCount.incrementAndGet();
		}

		void recordError() {
			errorCount.incrementAndGet();
		}

		void recordListenerError() {
			listenerErrorCount.incrementAndGet();
		}

		void recordListenerAdded(EventType type) {
			listenersPerType.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
		}

		void recordListenerRemoved(EventType type) {
			AtomicInteger count = listenersPerType.get(type);
			if (count != null) {
				count.decrementAndGet();
			}
		}

		void reset() {
			eventCount.set(0);
			filteredCount.set(0);
			errorCount.set(0);
			listenerErrorCount.set(0);
			totalProcessingTime.set(0);
			listenersPerType.clear();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"eventsProcessed", eventCount.get(),
					"eventsFiltered", filteredCount.get(),
					"errors", errorCount.get(),
					"listenerErrors", listenerErrorCount.get(),
					"averageProcessingTime", getAverageProcessingTime(),
					"listenersCount", listenersPerType);
		}

		private double getAverageProcessingTime() {
			long events = eventCount.get();
			return events > 0 ? (double) totalProcessingTime.get() / events : 0;
		}
	}
}
