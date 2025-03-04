package org.orgaprop.test7.security.diagnostic.monitoring;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moniteur de santé système qui surveille et analyse les métriques de
 * performance.
 * Cette classe maintient un historique circulaire des états de santé du système
 * et déclenche des alertes lorsque des seuils critiques sont atteints.
 */
public class SystemHealthMonitor {

	/** Logger pour les événements du moniteur de santé */
	private static final Logger logger = LoggerFactory.getLogger(SystemHealthMonitor.class);

	/** Historique circulaire des états de santé, limité aux 10 derniers états */
	private final CircularFifoQueue<HealthStatus> healthHistory = new CircularFifoQueue<>(10);

	private final MetricsConfig config;
	private final ConfigModule healthConfig;

	public SystemHealthMonitor() {
		this.config = MetricsConfig.getInstance();
		this.healthConfig = config.getModule("health");

		if (healthConfig == null) {
			logger.error("Configuration 'health' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}
	}

	/**
	 * Enregistre un nouvel état de santé du système et déclenche une analyse.
	 *
	 * @param memoryUsage Pourcentage d'utilisation de la mémoire (entre 0.0 et 1.0)
	 * @param historySize Taille actuelle de l'historique des diagnostics
	 * @param failureRate Taux d'échec des opérations (entre 0.0 et 1.0)
	 * @throws IllegalArgumentException si memoryUsage ou failureRate sont hors
	 *                                  limites
	 */
	public void recordHealth(double memoryUsage, int historySize, double failureRate) {
		HealthStatus status = new HealthStatus(memoryUsage, historySize, failureRate);
		healthHistory.add(status);
		analyzeHealth();
	}

	/**
	 * Analyse l'historique des états de santé pour détecter les tendances
	 * critiques.
	 * Déclenche une alerte si l'utilisation moyenne de la mémoire dépasse 85%.
	 */
	private void analyzeHealth() {
		double avgMemoryUsage = healthHistory.stream()
				.mapToDouble(HealthStatus::getMemoryUsage)
				.average()
				.orElse(0.0);

		// Utiliser la configuration centralisée
		double alertThreshold = (double) healthConfig.getProperty("alertThreshold");

		if (avgMemoryUsage > alertThreshold) {
			logger.warn("Tendance d'utilisation mémoire élevée: {}",
					String.format("%.2f%%", avgMemoryUsage * 100));
		}
	}

}
