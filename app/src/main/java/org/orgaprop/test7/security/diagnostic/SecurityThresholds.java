package org.orgaprop.test7.security.diagnostic;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Gère les seuils de sécurité de l'application.
 * Cette classe est thread-safe et immuable.
 */
public final class SecurityThresholds {
	/** Map des configurations de seuils */
	private static final Map<String, ThresholdConfig> THRESHOLDS = new ConcurrentHashMap<>();

	/**
	 * Configuration d'un seuil avec ses valeurs min et max.
	 */
	public static class ThresholdConfig {
		private final Object value;
		private final Object minValue;
		private final Object maxValue;

		/**
		 * Crée une nouvelle configuration de seuil.
		 * 
		 * @param value    Valeur actuelle
		 * @param minValue Valeur minimale autorisée
		 * @param maxValue Valeur maximale autorisée
		 * @throws IllegalArgumentException si la valeur est hors limites
		 */
		public ThresholdConfig(Object value, Object minValue, Object maxValue) {
			this.value = value;
			this.minValue = minValue;
			this.maxValue = maxValue;
			validateRange();
		}

		/**
		 * Vérifie que la valeur est dans les limites définies.
		 * 
		 * @throws IllegalArgumentException si la valeur est hors limites
		 */
		private void validateRange() {
			if (value instanceof Comparable) {
				Comparable<?> cValue = (Comparable<?>) value;
				if (cValue.compareTo(minValue) < 0 || cValue.compareTo(maxValue) > 0) {
					throw new IllegalArgumentException(
							String.format("Valeur %s hors limites [%s, %s]", value, minValue, maxValue));
				}
			}
		}
	}

	/** Taille minimale des clés de chiffrement */
	static final int MIN_KEY_SIZE = 256;
	/** Nombre maximum de tentatives échouées autorisées */
	static final int MAX_FAILED_ATTEMPTS = 5;
	/** Durée maximale d'une session (1 heure) */
	static final long MAX_SESSION_TIMEOUT = 3600000;
	/** Modes de chiffrement requis */
	static final String[] REQUIRED_CIPHER_MODES = { "GCM", "CCM" };

	static {
		validateThresholds();
	}

	/** Constructeur privé pour empêcher l'instanciation */
	private SecurityThresholds() {
	}

	/**
	 * Règle de validation pour les seuils de sécurité.
	 */
	public static class ValidationRule {
		private final String name;
		private final Predicate<Object> condition;
		private final String errorMessage;

		/**
		 * Crée une nouvelle règle de validation.
		 * 
		 * @param name         Nom de la règle
		 * @param condition    Condition à vérifier
		 * @param errorMessage Message d'erreur en cas d'échec
		 */
		private ValidationRule(String name, Predicate<Object> condition, String errorMessage) {
			this.name = name;
			this.condition = condition;
			this.errorMessage = errorMessage;
		}
	}

	/** Liste des règles de validation */
	private static final List<ValidationRule> RULES = Arrays.asList(
			new ValidationRule("KEY_SIZE",
					value -> ((Integer) value) >= MIN_KEY_SIZE,
					"La taille de clé doit être >= " + MIN_KEY_SIZE),
			new ValidationRule("MAX_ATTEMPTS",
					value -> ((Integer) value) <= MAX_FAILED_ATTEMPTS,
					"Le nombre maximal de tentatives doit être <= " + MAX_FAILED_ATTEMPTS));

	/**
	 * Valide tous les seuils configurés.
	 * 
	 * @throws IllegalStateException si une règle n'est pas respectée
	 */
	private static void validateThresholds() {
		for (ValidationRule rule : RULES) {
			if (!rule.condition.test(getValue(rule.name))) {
				throw new IllegalStateException(rule.errorMessage);
			}
		}
	}

	/**
	 * Récupère la valeur d'un seuil.
	 * 
	 * @param name Nom du seuil
	 * @return Valeur du seuil ou null si non trouvé
	 */
	private static Object getValue(String name) {
		ThresholdConfig config = THRESHOLDS.get(name);
		return config != null ? config.value : null;
	}
}
