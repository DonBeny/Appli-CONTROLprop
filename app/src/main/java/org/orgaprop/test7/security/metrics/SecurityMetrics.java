package org.orgaprop.test7.security.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityMetrics {
	private static final SecurityMetrics instance = new SecurityMetrics();

	private final AtomicInteger failedAttempts = new AtomicInteger();
	private final ConcurrentHashMap<String, Integer> failuresByType = new ConcurrentHashMap<>();

	private SecurityMetrics() {
	}

	public static SecurityMetrics getInstance() {
		return instance;
	}

	public void recordFailedAttempt(String type) {
		failedAttempts.incrementAndGet();
		failuresByType.merge(type, 1, Integer::sum);
	}

	public void reset() {
		failedAttempts.set(0);
		failuresByType.clear();
	}

	public int getFailedAttempts() {
		return failedAttempts.get();
	}

	public int getFailuresByType(String type) {
		return failuresByType.getOrDefault(type, 0);
	}
}
