package org.orgaprop.test7.security.diagnostic;

public class SecurityNotifier {
	private static final int MAX_HISTORY = 100;
	private final Queue<NotificationRecord> notificationHistory = new ConcurrentLinkedQueue<>();

	private static class NotificationRecord {
		final SecurityIssue issue;
		final LocalDateTime timestamp;
		final String status;

		NotificationRecord(SecurityIssue issue, String status) {
			this.issue = issue;
			this.timestamp = LocalDateTime.now();
			this.status = status;
		}
	}

	private void recordNotification(SecurityIssue issue, String status) {
		notificationHistory.offer(new NotificationRecord(issue, status));
		while (notificationHistory.size() > MAX_HISTORY) {
			notificationHistory.poll();
		}
	}

	private final Map<SecuritySeverity, List<SecurityIssueListener>> listenersByType = new EnumMap<>(
			SecuritySeverity.class);
	private final ExecutorService notificationExecutor = Executors.newSingleThreadExecutor();

	public void addListener(SecuritySeverity severity, SecurityIssueListener listener) {
		listenersByType.computeIfAbsent(severity, k -> new CopyOnWriteArrayList<>()).add(listener);
	}

	public void notifyIssue(SecurityIssue issue) {
		notificationExecutor.submit(() -> {
			List<SecurityIssueListener> listeners = listenersByType.get(issue.severity);
			if (listeners != null) {
				listeners.forEach(listener -> {
					try {
						listener.onCriticalIssue(issue);
					} catch (Exception e) {
						handleNotificationError(e, issue);
						retryNotification(issue);
					}
				});
			}
		});
	}

	private static final int ERROR_THRESHOLD = 5;
	private final Map<String, AtomicInteger> errorCounter = new ConcurrentHashMap<>();

	private void handleNotificationError(Exception e, SecurityIssue issue) {
		String errorType = e.getClass().getSimpleName();
		AtomicInteger count = errorCounter.computeIfAbsent(errorType, k -> new AtomicInteger());

		if (count.incrementAndGet() >= ERROR_THRESHOLD) {
			Log.e("SecurityNotifier", "Seuil d'erreurs atteint pour: " + errorType);
			updateIssueStatus(issue, NotificationStatus.FAILED);
			notifySystemAdmin(errorType, issue);
		} else {
			retryNotification(issue);
		}
	}

	private void notifySystemAdmin(String errorType, SecurityIssue issue) {
		// Logique de notification admin
		Log.w("SecurityNotifier", String.format(
				"ALERTE ADMIN - Type: %s, Issue: %s", errorType, issue.message));
	}

	public void shutdown() {
		notificationExecutor.shutdown();
		try {
			if (!notificationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				notificationExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final long RETRY_DELAY_MS = 1000;
	private final Map<SecurityIssue, Integer> retryCount = new ConcurrentHashMap<>();

	private void retryNotification(SecurityIssue issue) {
		int attempts = retryCount.getOrDefault(issue, 0);
		if (attempts < MAX_RETRY_ATTEMPTS) {
			retryCount.put(issue, attempts + 1);
			notificationExecutor.schedule(
					() -> notifyIssue(issue),
					RETRY_DELAY_MS * (attempts + 1),
					TimeUnit.MILLISECONDS);
		} else {
			recordNotification(issue, "ÉCHEC_DÉFINITIF");
		}
	}

	private final Map<SecurityIssue, NotificationStatus> issueStatus = new ConcurrentHashMap<>();

	private void updateIssueStatus(SecurityIssue issue, NotificationStatus status) {
		issueStatus.put(issue, status);
		recordNotification(issue, status.getLabel()); // Utilisation du getter
	}

	public NotificationStatus getIssueStatus(SecurityIssue issue) {
		return issueStatus.getOrDefault(issue, NotificationStatus.PENDING);
	}
}
