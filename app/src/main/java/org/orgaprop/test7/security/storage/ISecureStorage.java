package org.orgaprop.test7.security.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface ISecureStorage {
	void saveCredentials(@NonNull String username, @NonNull String encryptedPassword);

	@Nullable
	String getUsername();

	@Nullable
	String getEncryptedPassword();

	void clear();

	boolean hasCredentials();

	boolean backup();

	boolean restore();

	boolean verifyIntegrity();

	boolean migrateIfNeeded();
}
