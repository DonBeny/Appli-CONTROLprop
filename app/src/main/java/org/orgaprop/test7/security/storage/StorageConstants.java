package org.orgaprop.test7.security.storage;

public final class StorageConstants {
	public static final String PREF_NAME = "auth_prefs";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	public static final String KEY_BACKUP = "credentials_backup";
	public static final String KEY_VERSION = "storage_version";
	public static final int CURRENT_VERSION = 1;

	private StorageConstants() {
		// Empêche l'instanciation
	}
}