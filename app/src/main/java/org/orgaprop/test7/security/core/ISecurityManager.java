package org.orgaprop.test7.security.core;

import androidx.annotation.NonNull;
import org.orgaprop.test7.security.crypto.ICryptoProvider;
import org.orgaprop.test7.security.storage.ISecureStorage;
import org.orgaprop.test7.security.validation.ICredentialsValidator;

public interface ISecurityManager {
	ICryptoProvider getCryptoProvider();

	ISecureStorage getSecureStorage();

	ICredentialsValidator getCredentialsValidator();

	void initialize() throws SecurityException;

	void shutdown();

	boolean isInitialized();
}
