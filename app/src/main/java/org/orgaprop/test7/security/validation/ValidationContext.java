package org.orgaprop.test7.security.validation;

import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class ValidationContext {
	private static final long EXPIRATION_DELAY = TimeUnit.MINUTES.toMillis(30);
	private static final int MAX_FAILED_ATTEMPTS = 3;
	private static final long ATTEMPT_COOLDOWN = 1000; // 1 seconde

	private final Set<String> previousPasswords = new HashSet<>();
	private int failedAttempts = 0;
	private long lastAttemptTime = 0;

	public boolean isExpired() {
		return System.currentTimeMillis() - lastAttemptTime > EXPIRATION_DELAY;
	}

	public void reset() {
		failedAttempts = 0;
		lastAttemptTime = System.currentTimeMillis();
	}

	public boolean canAttempt() {
		long currentTime = System.currentTimeMillis();
		return failedAttempts < MAX_FAILED_ATTEMPTS &&
				(currentTime - lastAttemptTime) >= ATTEMPT_COOLDOWN;
	}

	public void recordFailedAttempt() {
		failedAttempts++;
		lastAttemptTime = System.currentTimeMillis();
	}

	public void recordSuccessfulAttempt() {
		failedAttempts = 0;
		lastAttemptTime = System.currentTimeMillis();
	}
}
