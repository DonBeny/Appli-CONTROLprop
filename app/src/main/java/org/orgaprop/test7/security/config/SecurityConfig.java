package org.orgaprop.test7.security.config;

public final class SecurityConfig {
	/** Paramètres de sécurité */
	public static final class Crypto {
		public static final int KEY_SIZE = 256;
		public static final String ALGORITHM = "AES";
		public static final String TRANSFORMATION = "AES/GCM/NoPadding";
	}

	/** Paramètres de validation */
	public static final class Validation {
		public static final int MAX_ATTEMPTS = 3;
		public static final long ATTEMPT_COOLDOWN = 1000L;
		public static final long SESSION_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
	}

	/** Paramètres de stockage */
	public static final class Storage {
		public static final int VERSION = 1;
		public static final String BACKUP_FORMAT = "yyyy-MM-dd HH:mm:ss";
	}

	/** Paramètres d'audit */
	public static final class Audit {
		public static final int MAX_ENTRIES = 1000;
		public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
		public static final boolean ENABLE_VERBOSE_LOGGING = false;
		public static final String AUDIT_FILE = "security_audit.log";
	}

	private SecurityConfig() {
		// Empêche l'instanciation
	}
}
