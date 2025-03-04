package org.orgaprop.test7.security.auth.tracking;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Calendar;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HourlyLoginTracker {
	private static final Logger logger = LoggerFactory.getLogger(HourlyLoginTracker.class);
	private final Map<Integer, AtomicInteger> hourlyStats = new ConcurrentHashMap<>();

	public void recordLogin(long timestamp) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		hourlyStats.computeIfAbsent(hour, k -> new AtomicInteger())
				.incrementAndGet();
		logger.debug("Login enregistré pour l'heure: {}", hour);
	}

	public Map<Integer, Integer> getDistribution() {
		return hourlyStats.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> e.getValue().get()));
	}

	public void clear() {
		hourlyStats.clear();
	}
}
