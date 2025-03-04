package org.orgaprop.test7.security.utils;

import androidx.annotation.NonNull;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecurityUtils {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private SecurityUtils() {
		// Empêche l'instanciation
	}

	public static String generateSecureToken(int length) {
		byte[] bytes = new byte[length];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static boolean isStrongPassword(@NonNull String password) {
		return password.length() >= 12 &&
				password.matches(".*[A-Z].*") &&
				password.matches(".*[a-z].*") &&
				password.matches(".*[0-9].*") &&
				password.matches(".*[^A-Za-z0-9].*");
	}

	public static void clearSensitiveData(char[] data) {
		if (data != null) {
			for (int i = 0; i < data.length; i++) {
				data[i] = '\0';
			}
		}
	}
}
