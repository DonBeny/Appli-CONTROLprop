package org.orgaprop.test7.security.monitoring;

import androidx.annotation.NonNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SecurityPerformanceMonitor {
	private static final SecurityPerformanceMonitor instance = new SecurityPerformanceMonitor();
	private final ConcurrentHashMap<String, AtomicLong> operationTimes = new ConcurrentHashMap<>();

	public static SecurityPerformanceMonitor getInstance() {
		return instance;
	}

	public void recordOperationTime(@NonNull String operation, long timeMs) {
		operationTimes.computeIfAbsent(operation, k -> new AtomicLong())
				.addAndGet(timeMs);
	}

	public long getAverageOperationTime(@NonNull String operation) {
		AtomicLong total = operationTimes.get(operation);
		return total != null ? total.get() : 0;
	}
}
