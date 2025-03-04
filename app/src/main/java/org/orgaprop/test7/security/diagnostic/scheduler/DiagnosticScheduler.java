package org.orgaprop.test7.security.diagnostic.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.orgaprop.test7.security.diagnostic.DiagnosticResult;

public class DiagnosticScheduler {
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final SecurityDiagnostic diagnostic;
	private final SecurityNotifier notifier;

	public void startPeriodicDiagnostic(long interval, TimeUnit unit) {
		scheduler.scheduleAtFixedRate(() -> {
			try {
				DiagnosticResult result = diagnostic.performSecurityCheck();
				if (!result.isSecure) {
					notifier.notifyIssues(result.issues);
				}
			} catch (Exception e) {
				// Gestion des erreurs
			}
		}, 0, interval, unit);
	}
}
