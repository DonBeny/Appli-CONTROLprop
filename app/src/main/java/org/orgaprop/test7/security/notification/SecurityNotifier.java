package org.orgaprop.test7.security.notification;

import androidx.annotation.NonNull;
import org.orgaprop.test7.security.events.SecurityEventManager;
import org.orgaprop.test7.security.events.SecurityEventManager.SecurityEvent;

public class SecurityNotifier {
	public static void notifySecurityEvent(@NonNull String type, @NonNull String message) {
		SecurityEvent event = new SecurityEvent(type, message);
		SecurityEventManager.dispatchEvent(event);
	}

	public static void notifyAuthenticationFailure(@NonNull String reason) {
		notifySecurityEvent("AUTH_FAILED", reason);
	}

	public static void notifySecurityViolation(@NonNull String details) {
		notifySecurityEvent("SECURITY_VIOLATION", details);
	}
}