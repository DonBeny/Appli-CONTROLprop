package org.orgaprop.test7.constants;

/**
 * Constantes utilisées dans MainActivity
 */
public final class MainActivityConstants {

	private MainActivityConstants() {
		// Empêche l'instanciation
	}

	/**
	 * Préférences de l'application
	 */
	public static final class Preferences {
		public static final String PREF_NAME_APPLI = "ControlProp";

		// Clés des préférences
		public static final String KEY_MBR = "mbr";
		public static final String KEY_PWD = "pwd";
		public static final String KEY_CURRENT_DATE = "current_date";
		public static final String KEY_LIMIT_TOP = "limit_top";
		public static final String KEY_LIMIT_DOWN = "limit_down";
		public static final String KEY_PLAN_ACTION = "plan_act";
		public static final String KEY_LIM_RAPPORT = "lim_rapport";
		public static final String KEY_DEST_RAPPORT = "dest_rapport";
		public static final String KEY_INFO_PROD = "info_prod";
		public static final String KEY_INFO_AFF = "info_aff";
		public static final String KEY_STRUCTURE_CTRL = "structure";
	}

	/**
	 * Codes de requête
	 */
	public static final class RequestCodes {
		public static final int UPDATE = 100;
		public static final int STORAGE = 200;
	}

	/**
	 * Configuration de l'application
	 */
	public static final class Config {
		public static final String ACCESS_CODE = "controlprop";
		public static final int DEFAULT_VERSION = 146;
		public static final boolean DEBUG_MODE = true;
	}

	/**
	 * États par défaut
	 */
	public static final class Defaults {
		public static final String DEFAULT_MBR_VALUE = "new";
		public static final String DEFAULT_MAC_VALUE = "new";
	}
}
