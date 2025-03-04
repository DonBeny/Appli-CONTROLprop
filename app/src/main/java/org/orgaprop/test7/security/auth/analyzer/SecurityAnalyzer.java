package org.orgaprop.test7.security.auth.analyzer;

import android.util.Log;

import org.orgaprop.test7.security.auth.LoginManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

public class SecurityAnalyzer {

	private static final String TAG = LoginManager.class.getSimpleName();
	private static final int MAX_IP_ATTEMPTS = 5;
	private static final long BLOCK_DURATION = TimeUnit.MINUTES.toMillis(30);

	private final Map<String, AtomicInteger> ipAttempts = new ConcurrentHashMap<>();
	private final Set<String> blockedIps = Collections.synchronizedSet(new HashSet<>());
	private final ScheduledExecutorService scheduledExecutor;
	private SecurityEventListener securityEventListener;

	public interface SecurityEventListener {
		void onSuspiciousActivity(String reason, Map<String, Object> details);

		void onIpBlocked(String ip);

		void onIpUnblocked(String ip);
	}

	public SecurityAnalyzer() {
		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	}

	public void setSecurityEventListener(SecurityEventListener listener) {
		this.securityEventListener = listener;
	}

	public void trackIpAttempt(String ip) {
		if (blockedIps.contains(ip)) {
			Log.w(TAG, "Tentative d'accès depuis une IP bloquée: "+ip);
			return;
		}

		AtomicInteger attempts = ipAttempts.computeIfAbsent(ip, k -> new AtomicInteger());
		if (attempts.incrementAndGet() > MAX_IP_ATTEMPTS) {
			blockIpAddress(ip);
		}
	}

	private void blockIpAddress(String ip) {
		blockedIps.add(ip);
		Log.w(TAG, "IP bloquée: "+ip);
		if (securityEventListener != null) {
			securityEventListener.onIpBlocked(ip);
		}
		scheduleIpUnblock(ip);
	}

	private void scheduleIpUnblock(String ip) {
		scheduledExecutor.schedule(() -> {
			blockedIps.remove(ip);
			ipAttempts.remove(ip);
			Log.i(TAG, "IP débloquée: "+ip);
			if (securityEventListener != null) {
				securityEventListener.onIpUnblocked(ip);
			}
		}, BLOCK_DURATION, TimeUnit.MILLISECONDS);
	}

	public void analyzeLoginPattern(String username, boolean success, long timestamp) {
		List<String> suspiciousPatterns = new ArrayList<>();

		// Analyse des patterns suspects
		if (!success && ipAttempts.get(username) != null &&
				ipAttempts.get(username).get() > MAX_IP_ATTEMPTS / 2) {
			suspiciousPatterns.add("Tentatives multiples échouées");
		}

		if (!suspiciousPatterns.isEmpty()) {
			Map<String, Object> details = new HashMap<>();
			details.put("username", username);
			details.put("timestamp", timestamp);
			details.put("patterns", suspiciousPatterns);

			if (securityEventListener != null) {
				securityEventListener.onSuspiciousActivity(
						"Activité suspecte détectée",
						details);
			}
		}
	}

	public void shutdown() {
		scheduledExecutor.shutdown();
		try {
			if (!scheduledExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
				scheduledExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduledExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}
