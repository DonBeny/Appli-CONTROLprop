package org.orgaprop.test7.security.diagnostic.metrics;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Représente une entrée de métrique pour le diagnostic de sécurité.
 * Cette classe est immuable et thread-safe.
 */
@Getter
public class MetricEntry {

	/** Horodatage de la création de la métrique */
	private final LocalDateTime timestamp;

	/** Nombre de problèmes de sécurité détectés */
	private final int issueCount;

	/** Temps de traitement en millisecondes */
	private final long processingTime;

	/** Indique si le diagnostic est sécurisé */
	private final boolean isSecure;

	/** Métadonnées associées à cette métrique */
	private final Map<String, Object> metadata;

	/**
	 * Crée une nouvelle entrée de métrique.
	 *
	 * @param timestamp      Horodatage de la création
	 * @param issueCount     Nombre de problèmes détectés
	 * @param processingTime Temps de traitement en ms
	 * @param isSecure       État de sécurité
	 * @param metadata       Métadonnées additionnelles
	 * @throws NullPointerException si timestamp ou metadata sont null
	 */
	public MetricEntry(LocalDateTime timestamp, int issueCount,
			long processingTime, boolean isSecure,
			Map<String, Object> metadata) {
		if (timestamp == null) {
			throw new NullPointerException("timestamp ne peut pas être null");
		}
		if (metadata == null) {
			throw new NullPointerException("metadata ne peut pas être null");
		}

		this.timestamp = timestamp;
		this.issueCount = issueCount;
		this.processingTime = processingTime;
		this.isSecure = isSecure;
		this.metadata = new HashMap<>(metadata);
	}
}
