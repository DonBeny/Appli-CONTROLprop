package org.orgaprop.test7.constants;

import android.content.pm.ActivityInfo;

/**
 * Constantes utilisées dans SelectActivity
 */
public final class SelectActivityConstants {

	private SelectActivityConstants() {
		// Empêche l'instanciation
	}

	/**
	 * Clés pour les extras des Intents
	 */
	public static final class Keys {
		public static final String TYPE = "type";
		public static final String AGENCES = "agcs";
		public static final String RESIDENCE = "rsd";
		public static final String PROXY = "proxi";
		public static final String CONTRA = "contra";
		public static final String COMMENT = "comment";
		public static final String LIST = "list";
		public static final String ID = "id";
		public static final String TEXT = "txt";
	}

	/**
	 * Configuration de l'activité
	 */
	public static final class Config {
		public static final int SCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		public static final boolean ENABLE_BACK_BUTTON = true;
		public static final long SEARCH_DELAY_MS = 300;
		public static final int MIN_SEARCH_LENGTH = 3;
	}

	/**
	 * Codes de résultat
	 */
	public static final class ResultCodes {
		public static final int SUCCESS = 1;
		public static final int ERROR = 0;
	}
}