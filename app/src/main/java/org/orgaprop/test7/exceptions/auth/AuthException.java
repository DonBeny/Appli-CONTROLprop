package org.orgaprop.test7.exceptions.auth;

import org.orgaprop.test7.exceptions.BaseException;
import org.orgaprop.test7.exceptions.config.ConfigException;

import java.util.Arrays;
import java.util.List;

/**
 * Exception levée pour les erreurs d'authentification.
 */
public class AuthException extends BaseException {

	private static final List<String> VALID_ERROR_CODES = Arrays.asList(
			ConfigException.AUTH_ERROR
	// Ajoutez d'autres codes d'erreur valides ici
	);

	/**
	 * Constructeur de AuthException.
	 *
	 * @param message   Le message d'erreur.
	 * @param errorCode Le code d'erreur.
	 */
	public AuthException(String message, String errorCode) {
		super(message, errorCode);
	}

	/**
	 * Constructeur de AuthException avec cause.
	 *
	 * @param message   Le message d'erreur.
	 * @param errorCode Le code d'erreur.
	 * @param cause     La cause de l'exception.
	 */
	public AuthException(String message, String errorCode, Throwable cause) {
		super(message, errorCode, cause);
	}

	/**
	 * Valide le code d'erreur.
	 *
	 * @param errorCode Le code d'erreur à valider.
	 * @return Le code d'erreur s'il est valide.
	 * @throws IllegalArgumentException si le code d'erreur n'est pas valide.
	 */
	@Override
	protected String validateErrorCode(String errorCode) {
		if (!VALID_ERROR_CODES.contains(errorCode)) {
			throw new IllegalArgumentException("Code d'erreur invalide : " + errorCode);
		}
		return errorCode;
	}

}
