package org.orgaprop.test7.exceptions.config;

import org.orgaprop.test7.exceptions.BaseException;

/**
 * Exception levée pour les erreurs de configuration.
 */
public class ConfigException extends BaseException {

	public static final String CONFIG_ERROR = "CONFIG_ERROR";
	public static final String NOTIFIER_ERROR = "NOTIFIER_ERROR";
	public static final String VALIDATOR_ERROR = "VALIDATOR_ERROR";
	public static final String AUTH_ERROR = "AUTH_ERROR";
	public static final String NETWORK_ERROR = "NETWORK_ERROR";

	public static final String ERR_MESS_BAD_DATA = "Une erreur s'est produite lors du traitement des données";

	public ConfigException(String message, String errorCode) {
		super(message, errorCode);
	}

	public ConfigException(String message, String errorCode, Throwable cause) {
		super(message, errorCode, cause);
	}

}
