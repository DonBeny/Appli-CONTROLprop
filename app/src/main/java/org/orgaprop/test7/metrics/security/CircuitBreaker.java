package org.orgaprop.test7.metrics.security;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitBreaker {
	private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

	private static final long DEFAULT_RESET_TIMEOUT = 60_000; // 1 minute
	private static final int DEFAULT_FAILURE_THRESHOLD = 3;

	private final AtomicInteger failureCount;
	private final AtomicInteger consecutiveFailures;
	private final AtomicBoolean isOpen;
	private volatile long lastFailureTime;
	private final long resetTimeout;
	private final int failureThreshold;
	private final Map<String, FailureStats> failureStats;
	private final List<CircuitBreakerListener> listeners;

	public CircuitBreaker() {
		this(DEFAULT_FAILURE_THRESHOLD, DEFAULT_RESET_TIMEOUT);
	}

	public CircuitBreaker(int failureThreshold, long resetTimeout) {
		this.failureCount = new AtomicInteger(0);
		this.consecutiveFailures = new AtomicInteger(0);
		this.isOpen = new AtomicBoolean(false);
		this.resetTimeout = resetTimeout;
		this.failureThreshold = failureThreshold;
		this.failureStats = new ConcurrentHashMap<>();
		this.listeners = new CopyOnWriteArrayList<>();
	}

	public boolean allowOperation() {
		if (isOpen.get()) {
			if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
				reset();
				return true;
			}
			return false;
		}
		return true;
	}

	public void recordFailure(String operation) {
		lastFailureTime = System.currentTimeMillis();
		int failures = consecutiveFailures.incrementAndGet();

		if (failures >= failureThreshold) {
			handleCriticalFailure(operation, failures);
		}

		failureStats.computeIfAbsent(operation, k -> new FailureStats())
				.recordFailure();
	}

	private void handleCriticalFailure(String operation, int failures) {
		isOpen.set(true);
		notifyListeners(CircuitBreakerEvent.OPENED);

		Map<String, Object> alertData = Map.of(
				"operation", operation,
				"failures", failures,
				"stats", getFailureStats());

		logger.error("Circuit breaker ouvert pour {}", operation);
	}

	public void reset() {
		isOpen.set(false);
		failureCount.set(0);
		consecutiveFailures.set(0);
		notifyListeners(CircuitBreakerEvent.RESET);
		logger.info("Circuit breaker réinitialisé");
	}

	public void addListener(CircuitBreakerListener listener) {
		listeners.add(listener);
	}

	private void notifyListeners(CircuitBreakerEvent event) {
		listeners.forEach(l -> l.onCircuitBreakerEvent(event));
	}

	public Map<String, Object> getFailureStats() {
		return Map.of(
				"isOpen", isOpen.get(),
				"failureCount", failureCount.get(),
				"consecutiveFailures", consecutiveFailures.get(),
				"lastFailureTime", lastFailureTime,
				"operationStats", new HashMap<>(failureStats));
	}
}
