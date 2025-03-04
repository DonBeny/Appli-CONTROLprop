package org.orgaprop.test7.security.diagnostic.entry;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.orgaprop.test7.security.diagnostic.DiagnosticResult;

/**
 * Représente une entrée de diagnostic avec son résultat et son contexte.
 * Cette classe est thread-safe et immuable pour ses champs principaux.
 */
private static class DiagnosticEntry {

	/** Horodatage de création de l'entrée */
	final LocalDateTime timestamp;

	/** Résultat du diagnostic associé */
	final DiagnosticResult result;

	/** Contexte dans lequel le diagnostic a été effectué */
	final String context;

	/** Nom du thread ayant créé cette entrée */
	final String threadName;

	/** Métadonnées additionnelles, initialisées de manière lazy */
	private volatile Map<String, Object> metadata;

	/**
	 * Crée une nouvelle entrée de diagnostic.
	 *
	 * @param result  Le résultat du diagnostic à enregistrer
	 * @param context Le contexte dans lequel le diagnostic a été effectué
	 * @throws NullPointerException si result est null
	 */
	DiagnosticEntry(DiagnosticResult result, String context) {
		this.timestamp = LocalDateTime.now();
		this.result = result;
		this.context = context != null ? context : "Sans contexte";
		this.threadName = Thread.currentThread().getName();
	}

	/**
	 * Ajoute une métadonnée à cette entrée.
	 * Les métadonnées sont initialisées de manière lazy lors du premier ajout.
	 *
	 * @param key   La clé de la métadonnée
	 * @param value La valeur de la métadonnée
	 * @throws NullPointerException si key est null
	 */
	public void addMetadata(String key, Object value) {
		if (metadata == null) {
			synchronized (this) {
				if (metadata == null) {
					metadata = new ConcurrentHashMap<>();
				}
			}
		}
		metadata.put(key, value);
	}

	/**
	 * Retourne une vue non modifiable des métadonnées.
	 *
	 * @return Une Map non modifiable des métadonnées, ou une Map vide si aucune
	 *         métadonnée n'existe
	 */
	public Map<String, Object> getMetadata() {
		return metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
	}
}
