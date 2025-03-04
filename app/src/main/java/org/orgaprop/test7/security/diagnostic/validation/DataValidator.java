package org.orgaprop.test7.security.diagnostic.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validateur de données configurable et thread-safe.
 * Permet de définir des règles de validation personnalisées pour différents
 * champs.
 */
public class DataValidator {
	private static final Logger logger = LoggerFactory.getLogger(DataValidator.class);
	private final Map<String, ValidationRule> validationRules;

	/**
	 * Représente une règle de validation avec son prédicat et son message d'erreur.
	 */
	private static class ValidationRule {
		final Predicate<Object> validation;
		final String errorMessage;

		ValidationRule(Predicate<Object> validation, String errorMessage) {
			this.validation = validation;
			this.errorMessage = errorMessage;
		}
	}

	/**
	 * Crée un nouveau validateur avec une map thread-safe.
	 */
	public DataValidator() {
		this.validationRules = Collections.synchronizedMap(new HashMap<>());
	}

	/**
	 * Ajoute une règle de validation pour un champ.
	 *
	 * @param field        Nom du champ à valider
	 * @param validation   Prédicat de validation
	 * @param errorMessage Message d'erreur en cas d'échec
	 * @throws NullPointerException si un des paramètres est null
	 */
	public void addValidation(String field, Predicate<Object> validation, String errorMessage) {
		if (field == null || validation == null || errorMessage == null) {
			throw new NullPointerException("Les paramètres ne peuvent pas être null");
		}
		validationRules.put(field, new ValidationRule(validation, errorMessage));
		logger.debug("Règle de validation ajoutée pour le champ: {}", field);
	}

	/**
	 * Valide les données selon les règles définies.
	 *
	 * @param data Map des données à valider
	 * @return Liste des messages d'erreur, vide si tout est valide
	 */
	public List<String> validate(Map<String, Object> data) {
		List<String> errors = new ArrayList<>();

		validationRules.forEach((field, rule) -> {
			Object value = data.get(field);
			if (value == null || !rule.validation.test(value)) {
				errors.add(String.format("%s: %s", field, rule.errorMessage));
				logger.warn("Validation échouée pour {}: {}", field, value);
			}
		});

		return errors;
	}

	/**
	 * Vérifie si toutes les validations passent.
	 *
	 * @param data Map des données à valider
	 * @return true si toutes les validations passent, false sinon
	 */
	public boolean isValid(Map<String, Object> data) {
		return validate(data).isEmpty();
	}
}
