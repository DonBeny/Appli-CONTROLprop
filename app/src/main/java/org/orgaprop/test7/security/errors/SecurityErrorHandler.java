package org.orgaprop.test7.security.errors;

import androidx.annotation.NonNull;
import org.orgaprop.test7.security.notification.SecurityNotifier;

public class SecurityErrorHandler {
	public static void handleSecurityError(@NonNull Exception e, @NonNull String context) {
		// Log l'erreur
		SecurityNotifier.notifySecurityViolation(
				String.format("Erreur de sécurité dans %s: %s", context, e.getMessage()));

		// Réinitialise l'état si nécessaire
		if (isRecoverableError(e)) {
			recoverFromError(context);
		}
	}

	private static boolean isRecoverableError(Exception e) {
		return !(e instanceof SecurityException) ||
				e.getMessage().contains("timeout");
	}

	private static void recoverFromError(String context) {
		// Logique de récupération spécifique au contexte
	}
}
