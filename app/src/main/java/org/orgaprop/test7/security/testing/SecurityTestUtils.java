package org.orgaprop.test7.security.testing;

import androidx.annotation.NonNull;
import org.orgaprop.test7.security.crypto.CryptoProvider;
import org.orgaprop.test7.security.validation.CredentialsValidator;

public class SecurityTestUtils {
	public static void validateSecurityComponents() {
		validateCryptoProvider();
		validateCredentialsValidator();
		validateSecureStorage();
	}

	private static void validateCryptoProvider() {
		try {
			CryptoProvider provider = new CryptoProvider();
			String testData = "Test Data";
			String encrypted = provider.encrypt(testData);
			String decrypted = provider.decrypt(encrypted);
			assert testData.equals(decrypted) : "Échec du test de chiffrement/déchiffrement";
		} catch (Exception e) {
			throw new RuntimeException("Échec de la validation du CryptoProvider", e);
		}
	}

	// Autres méthodes de validation...
}