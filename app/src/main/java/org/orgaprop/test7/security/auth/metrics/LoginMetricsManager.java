package org.orgaprop.test7.security.auth.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginMetricsManager {
	private static final Logger logger = LoggerFactory.getLogger(LoginMetricsManager.class);

	private final Map<String, AtomicInteger> loginMetrics = new ConcurrentHashMap<>();
	private final Map<String, Long> loginTimes = new ConcurrentHashMap<>();
	private final AtomicInteger loginAttempts = new AtomicInteger(0);
	private static final long METRICS_CLEANUP_THRESHOLD = 7 * 24 * 60 * 60 * 1000L; // 7 jours

	public void recordLoginAttempt(String username, boolean success, long timestamp) {
		loginTimes.put(username, timestamp);
		loginMetrics.computeIfAbsent(username, k -> new AtomicInteger())
				.addAndGet(success ? 1 : -1);

		if (success) {
			loginAttempts.set(0);
		} else {
			loginAttempts.incrementAndGet();
		}
	}

	public double calculateFailureRate() {
		long totalAttempts = 0;
		long failedAttempts = 0;
		for (AtomicInteger metric : loginMetrics.values()) {
			int value = metric.get();
			totalAttempts++;
			if (value < 0)
				failedAttempts++;
		}
		return totalAttempts > 0 ? (double) failedAttempts / totalAttempts : 0.0;
	}

	public long calculateAverageLoginTime() {
		if (loginTimes.isEmpty())
			return 0;
		return loginTimes.values().stream()
				.mapToLong(Long::longValue)
				.sum() / loginTimes.size();
	}

	public void cleanupOldMetrics() {
		long cutoffTime = System.currentTimeMillis() - METRICS_CLEANUP_THRESHOLD;
		loginTimes.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
		loginMetrics.keySet().removeIf(key -> !loginTimes.containsKey(key));
	}

	public int getLoginAttempts() {
		return loginAttempts.get();
	}

	public Map<String, Integer> getMetricsSnapshot() {
		Map<String, Integer> snapshot = new HashMap<>();
		loginMetrics.forEach((key, value) -> snapshot.put(key, value.get()));
		return snapshot;
	}
}
