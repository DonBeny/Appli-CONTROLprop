package org.orgaprop.test7.security.crypto;

public class CryptoProviderFactory {
	private static volatile CryptoProvider instance;

	public static synchronized CryptoProvider getInstance() throws Exception {
		if (instance == null) {
			instance = new CryptoProvider();
		}
		return instance;
	}

	public static void reset() {
		if (instance != null) {
			instance.reset();
			instance = null;
		}
	}
}