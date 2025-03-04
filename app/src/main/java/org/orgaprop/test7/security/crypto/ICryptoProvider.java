package org.orgaprop.test7.security.crypto;

import androidx.annotation.NonNull;

public interface ICryptoProvider {
	String encrypt(@NonNull String data) throws Exception;

	String decrypt(@NonNull String encryptedData) throws Exception;

	boolean verifyIntegrity(@NonNull byte[] data, @NonNull byte[] hmac);

	void reset();
}
