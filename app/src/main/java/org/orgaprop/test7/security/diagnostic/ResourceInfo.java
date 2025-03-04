package org.orgaprop.test7.security.diagnostic;

import lombok.Value;
import java.util.Arrays;
import java.time.Instant;

/**
 * Classe immuable représentant les informations d'une ressource système.
 * Stocke les métadonnées d'une ressource comme sa trace de création et son
 * horodatage.
 */
@Value
public class ResourceInfo {
	/** La ressource auto-fermable */
	AutoCloseable resource;

	/** Description textuelle de la ressource */
	String description;

	/** Trace de la pile d'appels lors de la création */
	String creationTrace;

	/** Horodatage de création en millisecondes depuis l'epoch */
	long creationTime;

	/**
	 * Crée une nouvelle instance avec les informations de la ressource.
	 * Capture automatiquement la trace de création et l'horodatage.
	 *
	 * @param resource    La ressource à gérer
	 * @param description Description de la ressource pour le débogage
	 * @throws IllegalArgumentException si la ressource est null
	 */
	public ResourceInfo(AutoCloseable resource, String description) {
		if (resource == null) {
			throw new IllegalArgumentException("La ressource ne peut pas être null");
		}
		this.resource = resource;
		this.description = description != null ? description : "Ressource sans description";
		this.creationTrace = Arrays.toString(Thread.currentThread().getStackTrace());
		this.creationTime = System.currentTimeMillis();
	}

	/**
	 * Retourne l'instant de création de la ressource.
	 * 
	 * @return Instant représentant le moment de création
	 */
	public Instant getCreationInstant() {
		return Instant.ofEpochMilli(creationTime);
	}
}
