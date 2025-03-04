package org.orgaprop.test7.security.audit;

import androidx.annotation.NonNull;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

public class SecurityAuditor {
	private static final int MAX_AUDIT_ENTRIES = 1000;
	private final ConcurrentLinkedQueue<AuditEntry> auditTrail = new ConcurrentLinkedQueue<>();

	public void logAuditEvent(@NonNull String eventType, @NonNull String details) {
		AuditEntry entry = new AuditEntry(eventType, details);
		auditTrail.offer(entry);

		while (auditTrail.size() > MAX_AUDIT_ENTRIES) {
			auditTrail.poll();
		}
	}

	public List<AuditEntry> getAuditTrail() {
		return new ArrayList<>(auditTrail);
	}

	private static class AuditEntry {
		final LocalDateTime timestamp;
		final String eventType;
		final String details;

		AuditEntry(String eventType, String details) {
			this.timestamp = LocalDateTime.now();
			this.eventType = eventType;
			this.details = details;
		}
	}
}
