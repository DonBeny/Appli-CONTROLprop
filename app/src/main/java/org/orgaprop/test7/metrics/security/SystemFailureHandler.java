package org.orgaprop.test7.metrics.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SystemFailureHandler {
	private static final Logger logger = LoggerFactory.getLogger(SystemFailureHandler.class);

	private static final int MAX_FAILURE_HISTORY = 50;
	private static final int CRITICAL_FAILURE_THRESHOLD = 5;

	private final AtomicInteger failureCount;
	private final Queue<FailureRecord> failureHistory;
	private final SystemRecoveryManager recoveryManager;
	private final AlertSystem alertSystem;

	public SystemFailureHandler(AlertSystem alertSystem) {
		this.failureCount = new AtomicInteger(0);
		this.failureHistory = new ConcurrentLinkedQueue<>();
		this.recoveryManager = new SystemRecoveryManager();
		this.alertSystem = alertSystem;
	}

	public void handleSystemFailure(String component, Exception e) {
		logger.error("Défaillance système dans le composant: {}", component, e);

		FailureRecord record = new FailureRecord(component, e);
		updateFailureHistory(record);

		int currentFailures = failureCount.incrementAndGet();

		if (currentFailures >= CRITICAL_FAILURE_THRESHOLD) {
			handleCriticalFailure(component, currentFailures);
		}

		notifyFailure(record);
	}

	private void handleCriticalFailure(String component, int failureCount) {
		Map<String, Object> alertData = new HashMap<>();
		alertData.put("component", component);
		alertData.put("failureCount", failureCount);
		alertData.put("failureHistory", getFailureHistory());

		alertSystem.raiseAlert("CRITICAL_SYSTEM_FAILURE", alertData);
		recoveryManager.initiateRecovery();
	}

	private void updateFailureHistory(FailureRecord record) {
		failureHistory.offer(record);
		while (failureHistory.size() > MAX_FAILURE_HISTORY) {
			failureHistory.poll();
		}
	}

	private void notifyFailure(FailureRecord record) {
		Map<String, Object> failureData = new HashMap<>();
		failureData.put("component", record.component);
		failureData.put("errorType", record.exception.getClass().getSimpleName());
		failureData.put("message", record.exception.getMessage());
		failureData.put("timestamp", record.timestamp);

		alertSystem.raiseAlert("SYSTEM_FAILURE", failureData);
	}

	public Map<String, Object> getFailureStats() {
		return Map.of(
				"totalFailures", failureCount.get(),
				"recentFailures", failureHistory.size(),
				"lastFailure", failureHistory.peek() != null ? failureHistory.peek().timestamp : 0);
	}

	private static class FailureRecord {
		final String component;
		final Exception exception;
		final long timestamp;

		FailureRecord(String component, Exception exception) {
			this.component = component;
			this.exception = exception;
			this.timestamp = System.currentTimeMillis();
		}
	}
}
