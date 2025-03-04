package org.orgaprop.test7.security.auth.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginReportGenerator {
	private static final Logger logger = LoggerFactory.getLogger(LoginReportGenerator.class);

	public Map<String, Object> generatePerformanceReport(Map<String, Long> loginTimes,
			Map<String, AtomicInteger> loginMetrics) {
		Map<String, Object> report = new HashMap<>();
		report.put("averageLoginTime", calculateAverageLoginTime(loginTimes));
		report.put("totalLogins", loginTimes.size());
		report.put("successRate", calculateSuccessRate(loginMetrics));
		report.put("timestamp", System.currentTimeMillis());

		logger.debug("Rapport de performance généré: {}", report);
		return report;
	}

	public Map<String, Object> generateDiagnosticReport(boolean networkStatus,
			boolean sessionStatus,
			int loginAttempts,
			long lastLoginTime) {
		Map<String, Object> report = new HashMap<>();
		report.put("networkStatus", networkStatus);
		report.put("sessionStatus", sessionStatus);
		report.put("loginAttempts", loginAttempts);
		report.put("lastLoginTime", lastLoginTime);
		report.put("timestamp", System.currentTimeMillis());

		logger.debug("Rapport de diagnostic généré: {}", report);
		return report;
	}

	public Map<String, Object> generateSecurityReport(Map<String, Integer> ipAttempts,
			Set<String> blockedIps,
			Map<String, List<String>> suspiciousActivities) {
		Map<String, Object> report = new HashMap<>();
		report.put("totalIpAttempts", ipAttempts.values().stream().mapToInt(Integer::intValue).sum());
		report.put("blockedIpsCount", blockedIps.size());
		report.put("suspiciousActivitiesCount", suspiciousActivities.size());
		report.put("timestamp", System.currentTimeMillis());

		logger.debug("Rapport de sécurité généré: {}", report);
		return report;
	}

	private double calculateAverageLoginTime(Map<String, Long> loginTimes) {
		if (loginTimes.isEmpty())
			return 0;
		return loginTimes.values().stream()
				.mapToLong(Long::longValue)
				.average()
				.orElse(0.0);
	}

	private double calculateSuccessRate(Map<String, AtomicInteger> loginMetrics) {
		if (loginMetrics.isEmpty())
			return 0;
		long totalAttempts = loginMetrics.size();
		long successfulAttempts = loginMetrics.values().stream()
				.filter(v -> v.get() > 0)
				.count();
		return (double) successfulAttempts / totalAttempts;
	}
}
