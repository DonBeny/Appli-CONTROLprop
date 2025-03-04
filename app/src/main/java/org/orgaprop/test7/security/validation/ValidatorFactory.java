package org.orgaprop.test7.security.validation;

public class ValidatorFactory {
	private static volatile CredentialsValidator instance;

	public static synchronized ICredentialsValidator getValidator() {
		if (instance == null) {
			instance = new CredentialsValidator();
		}
		return instance;
	}

	public static void reset() {
		if (instance != null) {
			instance.cleanupExpiredContexts();
			instance = null;
		}
	}
}
