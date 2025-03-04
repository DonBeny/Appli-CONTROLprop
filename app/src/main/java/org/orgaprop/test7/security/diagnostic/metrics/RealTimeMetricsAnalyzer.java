package org.orgaprop.test7.security.diagnostic.metrics;

// Imports Java standard
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Imports de bibliothèques tierces
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports internes au projet
import org.orgaprop.test7.security.diagnostic.UnifiedSystemMonitoring;
import org.orgaprop.test7.security.diagnostic.entry.DiagnosticEntry;
import org.orgaprop.test7.security.diagnostic.metrics.model.MetricEntry;
import org.orgaprop.test7.security.diagnostic.stats.PerformanceStats;

/**
 * Analyse en temps réel les métriques de performance.
 * Thread-safe et optimisé pour la détection d'anomalies.
 * Cette classe maintient un historique circulaire des temps de traitement
 * et détecte les anomalies de performance.
 */
public class RealTimeMetricsAnalyzer {

	/** Logger pour cette classe */
	private static final Logger logger = LoggerFactory.getLogger(RealTimeMetricsAnalyzer.class);

	/** File circulaire des derniers temps de traitement (en ms) */
	private final CircularFifoQueue<Double> processingTimes = new CircularFifoQueue<>(100);

	/** Compteur d'anomalies consécutives détectées */
	private final AtomicInteger anomalyCount = new AtomicInteger(0);

	/** Moniteur système pour la gestion des situations à risque */
	private final UnifiedSystemMonitoring systemMonitor;

	/**
	 * Classe interne gérant les métriques en temps réel.
	 * Maintient un historique des dernières métriques et analyse les tendances.
	 */
	private class RealTimeMetrics {
		/** File circulaire des métriques récentes */
		private final CircularFifoQueue<MetricEntry> recentMetrics = new CircularFifoQueue<>(100);

		/** Horodatage de la dernière mise à jour */
		private final AtomicLong lastUpdateTime = new AtomicLong();

		/**
		 * Enregistre une nouvelle métrique et déclenche son analyse.
		 *
		 * @param entry          L'entrée de diagnostic associée
		 * @param processingTime Le temps de traitement en millisecondes
		 * @throws NullPointerException si entry est null
		 */
		void recordMetric(DiagnosticEntry entry, long processingTime) {
			recentMetrics.add(new MetricEntry(
					entry.timestamp,
					entry.result.getIssues().size(),
					processingTime,
					entry.result.isSecure()));
			lastUpdateTime.set(System.currentTimeMillis());
			analyzeMetrics();
		}

		/**
		 * Analyse les métriques récentes pour détecter les dégradations de performance.
		 * Déclenche une alerte si le temps moyen de traitement dépasse 1 seconde.
		 */
		private void analyzeMetrics() {
			if (recentMetrics.isEmpty())
				return;

			double avgProcessingTime = recentMetrics.stream()
					.mapToLong(MetricEntry::getProcessingTime)
					.average()
					.orElse(0.0);

			if (avgProcessingTime > 1000) { // > 1 seconde
				logger.warn("Dégradation des performances détectée");
				systemMonitor.handleRiskySituation("Dégradation performances");
			}
		}
	}

	/**
	 * Analyse un nouveau temps de traitement et met à jour les statistiques.
	 * Déclenche la détection d'anomalies si nécessaire.
	 *
	 * @param processingTime Temps de traitement en millisecondes à analyser
	 */
	void analyzeProcessingTime(long processingTime) {
		processingTimes.add((double) processingTime);
		detectAnomalies();
	}

	/**
	 * Détecte les anomalies dans les temps de traitement récents.
	 * Une anomalie est détectée si la moyenne mobile dépasse 1 seconde.
	 * Trois anomalies consécutives déclenchent une alerte système.
	 *
	 * @throws Exception si une erreur survient pendant la détection
	 */
	private void detectAnomalies() {
		double average = calculateMovingAverage();
		try {
			if (average > 1000) { // > 1 seconde
				if (anomalyCount.incrementAndGet() >= 3) {
					systemMonitor.handleRiskySituation("Performance dégradée persistante");
					stats.recordFailure();
					logger.warn("Anomalie de performance détectée: {}ms", average);
				}
			} else {
				anomalyCount.set(0);
			}
		} catch (Exception e) {
			handleSystemError(e, "Détection anomalies");
		}
	}

}
