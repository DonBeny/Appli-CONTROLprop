package org.orgaprop.test7.security.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import org.json.JSONObject;

/**
 * Gestionnaire de stockage sécurisé.
 * Utilise EncryptedSharedPreferences pour le stockage chiffré des credentials.
 */
public class SecureStorage {
	/** Nom des préférences chiffrées */
	private static final String PREF_NAME = "auth_prefs";

	/** Clés pour les données stockées */
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";
	/** Clé pour le backup */
	private static final String KEY_BACKUP = "credentials_backup";

	/** Instance de SharedPreferences chiffrées */
	private final SharedPreferences securePrefs;

	/**
	 * Initialise le stockage sécurisé.
	 * Configure EncryptedSharedPreferences avec AES-256-GCM.
	 * 
	 * @param context Contexte Android (non null)
	 * @throws Exception si l'initialisation échoue
	 */
	public SecureStorage(@NonNull Context context) throws Exception {
		MasterKey masterKey = new MasterKey.Builder(context)
				.setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
				.build();

		securePrefs = EncryptedSharedPreferences.create(
				context,
				PREF_NAME,
				masterKey,
				EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
				EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
	}

	/**
	 * Sauvegarde les credentials de l'utilisateur.
	 * 
	 * @param username          Nom d'utilisateur (non null)
	 * @param encryptedPassword Mot de passe chiffré (non null)
	 */
	public void saveCredentials(@NonNull String username, @NonNull String encryptedPassword) {
		if (username == null || encryptedPassword == null) {
			throw new IllegalArgumentException("Les credentials ne peuvent pas être null");
		}

		SharedPreferences.Editor editor = securePrefs.edit();
		try {
			editor.putString(KEY_USERNAME, username);
			editor.putString(KEY_PASSWORD, encryptedPassword);
			editor.apply();
		} catch (Exception e) {
			editor.clear().apply();
			throw new SecurityException("Erreur lors de la sauvegarde des credentials", e);
		}
	}

	/**
	 * Récupère le nom d'utilisateur stocké.
	 * 
	 * @return Nom d'utilisateur ou null si non trouvé
	 */
	@Nullable
	public String getUsername() {
		return securePrefs.getString(KEY_USERNAME, null);
	}

	/**
	 * Récupère le mot de passe chiffré stocké.
	 * 
	 * @return Mot de passe chiffré ou null si non trouvé
	 */
	@Nullable
	public String getEncryptedPassword() {
		return securePrefs.getString(KEY_PASSWORD, null);
	}

	/**
	 * Efface toutes les données stockées.
	 * À utiliser lors de la déconnexion.
	 */
	public void clear() {
		SharedPreferences.Editor editor = securePrefs.edit();
		try {
			editor.clear();
			editor.apply();
			// Force le garbage collector pour nettoyer les données sensibles
			System.gc();
		} catch (Exception e) {
			throw new SecurityException("Erreur lors du nettoyage du stockage", e);
		}
	}

	/**
	 * Vérifie si des credentials sont stockés.
	 * 
	 * @return true si username et password sont présents
	 */
	public boolean hasCredentials() {
		return securePrefs.contains(KEY_USERNAME) &&
				securePrefs.contains(KEY_PASSWORD);
	}

	/**
	 * Met à jour le stockage chiffré si nécessaire.
	 * 
	 * @return true si une migration a été effectuée
	 */
	public boolean migrateIfNeeded() {
		int version = securePrefs.getInt("storage_version", 0);
		if (version < 1) {
			// Migration du stockage v0 vers v1
			clear();
			securePrefs.edit()
					.putInt("storage_version", 1)
					.apply();
			return true;
		}
		return false;
	}

	/**
	 * Crée une sauvegarde des credentials.
	 * 
	 * @return true si la sauvegarde est réussie
	 */
	public boolean backup() {
		try {
			String username = getUsername();
			String password = getEncryptedPassword();

			if (username == null || password == null) {
				return false;
			}

			JSONObject backup = new JSONObject();
			backup.put("username", username);
			backup.put("password", password);
			backup.put("timestamp", System.currentTimeMillis());

			securePrefs.edit()
					.putString(KEY_BACKUP, backup.toString())
					.apply();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Restaure les credentials depuis la sauvegarde.
	 * 
	 * @return true si la restauration est réussie
	 */
	public boolean restore() {
		try {
			String backupStr = securePrefs.getString(KEY_BACKUP, null);
			if (backupStr == null) {
				return false;
			}

			JSONObject backup = new JSONObject(backupStr);
			String username = backup.getString("username");
			String password = backup.getString("password");

			saveCredentials(username, password);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Vérifie l'intégrité des données stockées.
	 * 
	 * @return true si les données sont intègres
	 */
	public boolean verifyIntegrity() {
		try {
			if (!hasCredentials()) {
				return true;
			}
			String username = getUsername();
			String password = getEncryptedPassword();
			return username != null && password != null && !username.isEmpty();
		} catch (Exception e) {
			clear();
			return false;
		}
	}
}
