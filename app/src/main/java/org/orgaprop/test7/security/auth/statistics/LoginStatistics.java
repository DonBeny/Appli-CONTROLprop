package org.orgaprop.test7.security.auth.statistics;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginStatistics {
	private static final Logger logger = LoggerFactory.getLogger(LoginStatistics.class);
	private final HourlyLoginStats hourlyStats;
	private final NetworkErrorStats networkErrorStats;
	private final Map<String, Long> loginTimes;

	public LoginStatistics() {
		this.hourlyStats = new HourlyLoginStats();
		this.networkErrorStats = new NetworkErrorStats();
		this.loginTimes = new ConcurrentHashMap<>();
	}

	public void recordLogin(String username, long timestamp, boolean success) {
		loginTimes.put(username, timestamp);
		hourlyStats.recordLogin(timestamp);
		if (!success) {
			networkErrorStats.recordError("LOGIN_FAILED");
		}
		logger.debug("Login enregistré: utilisateur={}, succès={}", username, success);
	}

	public Map<Integer, Integer> getHourlyDistribution() {
		return hourlyStats.getDistribution();
	}

	public Map<String, Integer> getErrorDistribution() {
		return networkErrorStats.getErrorDistribution();
	}

	public double getAverageLoginTime() {
		if (loginTimes.isEmpty())
			return 0;
		return loginTimes.values().stream()
				.mapToLong(Long::longValue)
				.average()
				.orElse(0.0);
	}

	private static class HourlyLoginStats {
		private final Map<Integer, AtomicInteger> hourlyDistribution = new ConcurrentHashMap<>();

		void recordLogin(long timestamp) {
			int hour = LocalDateTime.ofInstant(
					java.time.Instant.ofEpochMilli(timestamp),
					ZoneId.systemDefault()).getHour();

			hourlyDistribution.computeIfAbsent(hour, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<Integer, Integer> getDistribution() {
			Map<Integer, Integer> distribution = new HashMap<>();
			hourlyDistribution.forEach((hour, count) -> distribution.put(hour, count.get()));
			return distribution;
		}
	}

	private static class NetworkErrorStats {
		private final Map<String, AtomicInteger> errorsByType = new ConcurrentHashMap<>();

		void recordError(String type) {
			errorsByType.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Integer> getErrorDistribution() {
			Map<String, Integer> distribution = new HashMap<>();
			errorsByType.forEach((type, count) -> distribution.put(type, count.get()));
			return distribution;
		}
	}
}
