package org.orgaprop.test7.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.orgaprop.test7.exceptions.auth.core.AuthenticationException;
import org.orgaprop.test7.exceptions.auth.types.ErrorType;
import org.orgaprop.test7.security.crypto.CryptoProvider;
import org.orgaprop.test7.security.storage.SecureStorage;
import org.orgaprop.test7.security.validation.CredentialsValidator;

/**
 * Gestionnaire de l'authentification.
 * Gère le chiffrement, le stockage et la validation des credentials.
 */
public class AuthenticationManager {
	private static final String TAG = AuthenticationManager.class.getSimpleName();

	private final Context context;
	private final SecureStorage secureStorage;
	private final CryptoProvider cryptoProvider;
	private final CredentialsValidator validator;

	public AuthenticationManager(@NonNull Context context) throws AuthenticationException {
		try {
			this.context = context;
			this.secureStorage = new SecureStorage(context);
			this.cryptoProvider = new CryptoProvider();
			this.validator = new CredentialsValidator();
			loadCredentials();
		} catch (Exception e) {
			throw new AuthenticationException(
					ErrorType.INITIALIZATION_ERROR,
					"Échec de l'initialisation du gestionnaire d'authentification").withSystemInfo();
		}
	}

	public void saveCredentials(@NonNull String username, @NonNull String password)
			throws AuthenticationException {
		try {
			validator.validateCredentials(username, password);
			String encryptedPassword = cryptoProvider.encrypt(password);
			secureStorage.saveCredentials(username, encryptedPassword);
		} catch (Exception e) {
			throw new AuthenticationException(
					ErrorType.STORAGE_ERROR,
					"Échec de la sauvegarde des credentials").withAuthInfo("local_storage");
		}
	}

	@Nullable
	public String getUsername() {
		return secureStorage.getUsername();
	}

	public boolean validateCredentials() {
		try {
			String storedUsername = secureStorage.getUsername();
			String encryptedPassword = secureStorage.getEncryptedPassword();

			if (storedUsername == null || encryptedPassword == null) {
				return false;
			}

			String decryptedPassword = cryptoProvider.decrypt(encryptedPassword);
			return validator.isValid(storedUsername, decryptedPassword);
		} catch (Exception e) {
			Log.e(TAG, "Validation des credentials échouée", e);
			return false;
		}
	}

	public void clearCredentials() {
		secureStorage.clear();
		cryptoProvider.reset();
	}
}
