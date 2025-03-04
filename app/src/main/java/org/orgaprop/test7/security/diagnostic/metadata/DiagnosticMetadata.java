package org.orgaprop.test7.security.diagnostic.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Fournit des métadonnées système pour les diagnostics de sécurité.
 * Cette classe utilitaire expose des méthodes statiques pour collecter
 * des informations sur l'état du système.
 */
public class DiagnosticMetadata {

	/**
	 * Collecte les métadonnées système actuelles.
	 * Les métadonnées incluent:
	 * <ul>
	 * <li>Nombre de processeurs disponibles</li>
	 * <li>Mémoire libre</li>
	 * <li>Mémoire totale</li>
	 * <li>Nombre de threads actifs</li>
	 * </ul>
	 *
	 * @return Une Map contenant les métadonnées système.
	 *         Les clés sont des String et les valeurs peuvent être de différents
	 *         types.
	 */
	public static Map<String, Object> getSystemMetadata() {
		Runtime runtime = Runtime.getRuntime();
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("availableProcessors", runtime.availableProcessors());
		metadata.put("freeMemory", runtime.freeMemory());
		metadata.put("totalMemory", runtime.totalMemory());
		metadata.put("threadCount", Thread.activeCount());
		return metadata;
	}

}
