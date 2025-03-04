package org.orgaprop.test7.security.validation;

import androidx.annotation.NonNull;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import org.orgaprop.test7.exceptions.auth.core.AuthenticationException;
import org.orgaprop.test7.exceptions.auth.types.ErrorType;

/**
 * Validateur des identifiants utilisateur.
 * Vérifie le format et la complexité des credentials.
 */
public class CredentialsValidator implements ICredentialsValidator {
	/** Longueur minimale pour le nom d'utilisateur */
	private static final int MIN_USERNAME_LENGTH = 3;

	/** Longueur maximale du nom d'utilisateur */
	private static final int MAX_USERNAME_LENGTH = 100;

	/** Longueur minimale pour le mot de passe */
	private static final int MIN_PASSWORD_LENGTH = 4;

	/** Longueur maximale du mot de passe */
	private static final int MAX_PASSWORD_LENGTH = 128;

	/** Message pour longueur maximale dépassée */
	private static final String MSG_USERNAME_TOO_LONG = "Le nom d'utilisateur ne peut pas dépasser %d caractères";
	private static final String MSG_PASSWORD_TOO_LONG = "Le mot de passe ne peut pas dépasser %d caractères";

	/** Liste des mots de passe courants interdits */
	private static final Set<String> FORBIDDEN_PASSWORDS = new HashSet<>(Arrays.asList(
			"password123", "12345678", "qwerty123"));

	/**
	 * Regex pour la validation du nom d'utilisateur : lettres, chiffres, points,
	 * tirets
	 */
	private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

	/**
	 * Regex pour la validation du mot de passe : au moins 1 chiffre, 1 minuscule, 1
	 * majuscule, 1 caractère spécial
	 */
	private static final Pattern PASSWORD_PATTERN = Pattern.compile(
			"^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$");

	/** Regex pour détecter les séquences répétées */
	private static final Pattern REPEATED_PATTERN = Pattern.compile("(.)\\1{2,}");

	/** Contexte de validation */
	private static final class ValidationContext {
		private final Set<String> previousPasswords = new HashSet<>();
		private int failedAttempts = 0;
		private long lastAttemptTime = 0;
	}

	private final Map<String, ValidationContext> contexts = new HashMap<>();

	/**
	 * Vérifie le contexte de validation pour un utilisateur.
	 * 
	 * @param username Nom d'utilisateur
	 * @return true si la validation est autorisée
	 */
	private boolean checkValidationContext(String username) {
		ValidationContext context = contexts.computeIfAbsent(
				username, k -> new ValidationContext());

		long currentTime = System.currentTimeMillis();
		if (currentTime - context.lastAttemptTime < 1000) { // Rate limiting
			context.failedAttempts++;
			return false;
		}

		if (context.failedAttempts >= 3) { // Max attempts
			return false;
		}

		context.lastAttemptTime = currentTime;
		return true;
	}

	/**
	 * Valide le format des identifiants.
	 * Vérifie la longueur et le format selon les patterns définis.
	 * 
	 * @param username Nom d'utilisateur à valider (non null)
	 * @param password Mot de passe à valider (non null)
	 * @throws AuthenticationException si la validation échoue avec le détail de
	 *                                 l'erreur
	 */
	public void validateCredentials(@NonNull String username, @NonNull String password)
			throws AuthenticationException {
		if (username == null || password == null) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Les identifiants ne peuvent pas être null");
		}

		if (username.length() < MIN_USERNAME_LENGTH) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Le nom d'utilisateur doit contenir au moins " + MIN_USERNAME_LENGTH + " caractères");
		}

		if (username.length() > MAX_USERNAME_LENGTH) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					String.format(MSG_USERNAME_TOO_LONG, MAX_USERNAME_LENGTH));
		}

		if (!USERNAME_PATTERN.matcher(username).matches()) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Le nom d'utilisateur contient des caractères invalides");
		}

		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères");
		}

		if (password.length() > MAX_PASSWORD_LENGTH) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					String.format(MSG_PASSWORD_TOO_LONG, MAX_PASSWORD_LENGTH));
		}

		if (!PASSWORD_PATTERN.matcher(password).matches()) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Le mot de passe ne respecte pas les critères de complexité");
		}

		validatePasswordStrength(password);
	}

	/**
	 * Vérifie les critères de sécurité additionnels.
	 * 
	 * @throws AuthenticationException si le mot de passe est trop faible
	 */
	private void validatePasswordStrength(String password) throws AuthenticationException {
		if (FORBIDDEN_PASSWORDS.contains(password.toLowerCase())) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Ce mot de passe est trop commun");
		}

		if (REPEATED_PATTERN.matcher(password).find()) {
			throw new AuthenticationException(ErrorType.VALIDATION_ERROR,
					"Le mot de passe ne doit pas contenir de séquences répétées");
		}
	}

	/**
	 * Vérifie si les identifiants sont valides.
	 * Méthode utilitaire retournant un booléen plutôt qu'une exception.
	 * 
	 * @param username Nom d'utilisateur à vérifier (non null)
	 * @param password Mot de passe à vérifier (non null)
	 * @return true si les identifiants sont valides, false sinon
	 */
	public boolean isValid(@NonNull String username, @NonNull String password) {
		try {
			validateCredentials(username, password);
			return true;
		} catch (AuthenticationException e) {
			return false;
		}
	}

	/**
	 * Nettoie les contextes de validation expirés.
	 */
	public void cleanupExpiredContexts() {
		contexts.entrySet().removeIf(entry -> entry.getValue().isExpired());
	}
}