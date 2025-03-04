package org.orgaprop.test7.exceptions;

import android.util.Log;

/**
 * Classe de base pour toutes les exceptions personnalisées de l'application.
 */
public class BaseException extends Exception {

	private static final String TAG = BaseException.class.getSimpleName();

	private final String errorCode;
	private static final long serialVersionUID = 1L;

	/**
	 * Constructeur de BaseException.
	 *
	 * @param message   Le message d'erreur.
	 * @param errorCode Le code d'erreur.
	 */
	public BaseException(String message, String errorCode) {
		super(message);
		this.errorCode = validateErrorCode(errorCode);
	}

	/**
	 * Constructeur de BaseException avec cause.
	 *
	 * @param message   Le message d'erreur.
	 * @param errorCode Le code d'erreur.
	 * @param cause     La cause de l'exception.
	 */
	public BaseException(String message, String errorCode, Throwable cause) {
		super(message, cause);
		this.errorCode = validateErrorCode(errorCode);
	}

	/**
	 * Retourne le code d'erreur.
	 *
	 * @return Le code d'erreur.
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Valide le code d'erreur.
	 *
	 * @param errorCode Le code d'erreur à valider.
	 * @return Le code d'erreur s'il est valide.
	 * @throws IllegalArgumentException si le code d'erreur n'est pas valide.
	 */
	protected String validateErrorCode(String errorCode) {
		// Par défaut, tous les codes d'erreur sont valides.
		// Les sous-classes peuvent redéfinir cette méthode pour ajouter des validations spécifiques.
		return errorCode;
	}

}