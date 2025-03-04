package org.orgaprop.test7.security.metrics;

import androidx.annotation.NonNull;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityStats {
	private static final SecurityStats instance = new SecurityStats();
	private final Map<String, AtomicLong> stats = new ConcurrentHashMap<>();

	public static SecurityStats getInstance() {
		return instance;
	}

	public void incrementStat(@NonNull String key) {
		stats.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
	}

	public long getStat(@NonNull String key) {
		return stats.getOrDefault(key, new AtomicLong(0)).get();
	}

	public void reset() {
		stats.clear();
	}
}
