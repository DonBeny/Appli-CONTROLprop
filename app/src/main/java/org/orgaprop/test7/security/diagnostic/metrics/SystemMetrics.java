package org.orgaprop.test7.security.diagnostic.metrics;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gère les métriques système du diagnostic.
 * Thread-safe grâce à l'utilisation de compteurs atomiques.
 * Cette classe maintient différents compteurs pour suivre les performances du
 * système.
 */
private class SystemMetrics {

	/** Temps total de traitement en millisecondes */
	private final AtomicLong totalProcessingTime = new AtomicLong();

	/** Nombre total d'entrées traitées */
	private final AtomicInteger processedEntries = new AtomicInteger();

	/** Nombre d'opérations de maintenance effectuées */
	private final AtomicInteger maintenanceCount = new AtomicInteger();

	/**
	 * Enregistre une nouvelle métrique selon son type.
	 * 
	 * @param type  Le type de métrique à enregistrer
	 * @param value La valeur à ajouter pour cette métrique
	 * @throws NullPointerException si type est null
	 */
	void recordMetric(MetricType type, long value) {
		switch (type) {
			case PROCESSING_TIME -> totalProcessingTime.addAndGet(value);
			case ENTRY_PROCESSED -> processedEntries.incrementAndGet();
			case MAINTENANCE -> maintenanceCount.incrementAndGet();
		}
	}

	/**
	 * Retourne une Map contenant toutes les métriques actuelles.
	 * Les métriques incluent :
	 * <ul>
	 * <li>avgProcessingTime : temps moyen de traitement</li>
	 * <li>maintenanceCount : nombre de maintenances</li>
	 * <li>processedEntries : nombre d'entrées traitées</li>
	 * </ul>
	 *
	 * @return Map non modifiable des métriques actuelles
	 */
	Map<String, Number> getMetrics() {
		return Map.of(
				"avgProcessingTime", getAverageProcessingTime(),
				"maintenanceCount", maintenanceCount.get(),
				"processedEntries", processedEntries.get());
	}

	/**
	 * Calcule le temps moyen de traitement par entrée.
	 *
	 * @return Le temps moyen en millisecondes, ou 0 si aucune entrée n'a été
	 *         traitée
	 */
	private double getAverageProcessingTime() {
		int count = processedEntries.get();
		return count > 0 ? (double) totalProcessingTime.get() / count : 0;
	}

}
