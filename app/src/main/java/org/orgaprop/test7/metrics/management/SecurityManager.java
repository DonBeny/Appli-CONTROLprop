package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class SecurityManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

	private static final int MAX_FAILED_ATTEMPTS = 3;
	private static final long LOCKOUT_DURATION = 300_000; // 5 minutes
	private static final long CHECK_INTERVAL = 60_000; // 1 minute

	private final Map<String, SecurityContext> securityContexts;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final SecurityMetrics metrics;
	private final AlertManager alertManager;

	public SecurityManager(AlertManager alertManager) {
		this.securityContexts = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new SecurityMetrics();
		this.alertManager = alertManager;

		startSecurityMonitoring();
	}

	private ScheduledExecutorService createScheduler() {
		return Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "security-monitor");
			t.setDaemon(true);
			return t;
		});
	}

	private void startSecurityMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkSecurityStatus,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	public void recordSecurityEvent(String userId, SecurityEventType eventType) {
		if (!isRunning.get()) {
			logger.warn("Tentative d'enregistrement alors que le manager est arrêté");
			return;
		}

		try {
			SecurityContext context = getOrCreateContext(userId);
			context.recordEvent(eventType);
			metrics.recordEvent(eventType);

			if (shouldTriggerAlert(context)) {
				handleSecurityViolation(userId, context);
			}
		} catch (Exception e) {
			handleEventError(e, userId, eventType);
		}
	}

	private SecurityContext getOrCreateContext(String userId) {
		return securityContexts.computeIfAbsent(userId,
				k -> new SecurityContext(userId));
	}

	private boolean shouldTriggerAlert(SecurityContext context) {
		return context.getFailedAttempts() >= MAX_FAILED_ATTEMPTS ||
				context.hasAnomalousActivity();
	}

	private void handleSecurityViolation(String userId, SecurityContext context) {
		context.lockAccount(LOCKOUT_DURATION);
		Map<String, Object> alertData = Map.of(
				"userId", userId,
				"failedAttempts", context.getFailedAttempts(),
				"lastEventTime", context.getLastEventTime(),
				"lockoutDuration", LOCKOUT_DURATION);
		alertManager.raiseAlert("SECURITY_VIOLATION", alertData);
	}

	private void checkSecurityStatus() {
		try {
			long now = System.currentTimeMillis();
			securityContexts.values().forEach(context -> {
				if (context.isLocked() && now >= context.getLockExpiration()) {
					unlockAccount(context.getUserId());
				}
				if (context.hasAnomalousActivity()) {
					reportAnomalousActivity(context);
				}
			});
			cleanupOldContexts();
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification de la sécurité", e);
		}
	}

	private void unlockAccount(String userId) {
		SecurityContext context = securityContexts.get(userId);
		if (context != null) {
			context.unlock();
			metrics.recordUnlock(userId);
			logger.info("Compte déverrouillé : {}", userId);
		}
	}

	public boolean isAccountLocked(String userId) {
		SecurityContext context = securityContexts.get(userId);
		return context != null && context.isLocked();
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
			cleanup();
		}
	}

	private static class SecurityContext {
		private final String userId;
		private final AtomicInteger failedAttempts;
		private final ConcurrentLinkedQueue<Long> eventTimestamps;
		private volatile long lockExpiration;

		SecurityContext(String userId) {
			this.userId = userId;
			this.failedAttempts = new AtomicInteger(0);
			this.eventTimestamps = new ConcurrentLinkedQueue<>();
			this.lockExpiration = 0;
		}

		void recordEvent(SecurityEventType eventType) {
			eventTimestamps.offer(System.currentTimeMillis());
			if (eventType == SecurityEventType.FAILED_ATTEMPT) {
				failedAttempts.incrementAndGet();
			} else if (eventType == SecurityEventType.SUCCESSFUL_ATTEMPT) {
				failedAttempts.set(0);
			}
		}

		boolean hasAnomalousActivity() {
			return calculateEventFrequency() > 10; // Plus de 10 événements par minute
		}

		private double calculateEventFrequency() {
			long now = System.currentTimeMillis();
			long oneMinuteAgo = now - 60_000;
			return eventTimestamps.stream()
					.filter(timestamp -> timestamp > oneMinuteAgo)
					.count();
		}

		void lockAccount(long duration) {
			lockExpiration = System.currentTimeMillis() + duration;
		}

		void unlock() {
			lockExpiration = 0;
			failedAttempts.set(0);
		}

		boolean isLocked() {
			return System.currentTimeMillis() < lockExpiration;
		}

		String getUserId() {
			return userId;
		}

		int getFailedAttempts() {
			return failedAttempts.get();
		}

		long getLastEventTime() {
			return eventTimestamps.peek() != null ? eventTimestamps.peek() : 0;
		}

		long getLockExpiration() {
			return lockExpiration;
		}
	}

	private enum SecurityEventType {
		FAILED_ATTEMPT, SUCCESSFUL_ATTEMPT, SUSPICIOUS_ACTIVITY
	}

	private static class SecurityMetrics {
		private final Map<String, AtomicInteger> eventsByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> locksByUser = new ConcurrentHashMap<>();
		private final AtomicLong totalEvents = new AtomicLong(0);
		private final AtomicLong totalUnlocks = new AtomicLong(0);

		void recordEvent(SecurityEventType type) {
			eventsByType.computeIfAbsent(type.name(), k -> new AtomicInteger())
					.incrementAndGet();
			totalEvents.incrementAndGet();
		}

		void recordUnlock(String userId) {
			totalUnlocks.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalEvents", totalEvents.get(),
					"eventsByType", new HashMap<>(eventsByType),
					"locksByUser", new HashMap<>(locksByUser),
					"totalUnlocks", totalUnlocks.get());
		}
	}
}
