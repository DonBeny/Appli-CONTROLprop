package org.orgaprop.test7.security.diagnostic.state;

import java.util.concurrent.atomic.*;

/**
 * Gère l'état du système de diagnostic et la maintenance.
 * Cette classe est thread-safe grâce à l'utilisation de variables atomiques.
 */
public class SystemState {

	/** Horodatage de la dernière maintenance effectuée */
	private final AtomicLong lastMaintenanceTime = new AtomicLong(System.currentTimeMillis());

	/** Indicateur de besoin de maintenance */
	private final AtomicBoolean needsMaintenance = new AtomicBoolean(false);

	/** Compteur d'échecs consécutifs de maintenance */
	private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

	/**
	 * Enregistre un échec de maintenance et déclenche le protocole d'urgence
	 * si le nombre d'échecs consécutifs atteint 3.
	 */
	public void recordMaintenanceFailure() {
		if (consecutiveFailures.incrementAndGet() >= 3) {
			SystemMonitor.triggerEmergencyProtocol();
		}
	}

	/**
	 * Enregistre une maintenance réussie.
	 * Réinitialise le compteur d'échecs et met à jour l'horodatage.
	 */
	public void maintenanceSuccess() {
		consecutiveFailures.set(0);
		lastMaintenanceTime.set(System.currentTimeMillis());
		needsMaintenance.set(false);
	}

	/**
	 * Vérifie si une maintenance est nécessaire.
	 *
	 * @return true si une maintenance est requise, false sinon
	 */
	public boolean needsMaintenance() {
		return needsMaintenance.get();
	}

}
