package org.orgaprop.test7.security.monitoring;

public interface ISecurityMonitor {
	void logFailedAttempt(String username, String reason);

	void logSuccessfulAttempt(String username);

	void logSecurityEvent(String event, String details);

	void checkSecurityStatus();
}
