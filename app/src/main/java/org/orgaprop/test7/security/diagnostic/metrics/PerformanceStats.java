package org.orgaprop.test7.security.diagnostic.metrics;

// Imports Java standard
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Imports pour le logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports internes au projet
import org.orgaprop.test7.security.diagnostic.monitoring.UnifiedSystemMonitoring;

public class PerformanceStats {
	private static final Logger logger = LoggerFactory.getLogger(PerformanceStats.class);

	private final AtomicInteger processedEntries = new AtomicInteger();
	private final AtomicLong totalProcessingTime = new AtomicLong();
	private final AtomicInteger failedProcessings = new AtomicInteger();
	private final AtomicInteger criticalEvents = new AtomicInteger();
	private final CircularFifoQueue<OperationMetric> recentOperations;
	private final MetricsConfig config;
	private final ConfigModule performanceConfig;
	private final PerformanceTracker tracker;

	public PerformanceStats() {
		this.config = MetricsConfig.getInstance();
		this.performanceConfig = config.getModule("performance_stats");

		if (performanceConfig == null) {
			logger.error("Configuration 'performance_stats' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> monitoringConfig = (Map<String, Object>) performanceConfig.getProperty("monitoring");
		int recentOperationsSize = (int) monitoringConfig.get("recentOperationsSize");
		this.recentOperations = new CircularFifoQueue<>(recentOperationsSize);
		this.tracker = new PerformanceTracker();
	}

	private class PerformanceTracker {
		private final AtomicInteger consecutiveSlowOperations = new AtomicInteger();
		private final Map<String, MovingAverage> operationTimes = new ConcurrentHashMap<>();
		private final Map<String, Object> monitoringConfig;
		private final Map<String, Object> thresholds;

		PerformanceTracker() {
			this.monitoringConfig = (Map<String, Object>) performanceConfig.getProperty("monitoring");
			this.thresholds = (Map<String, Object>) performanceConfig.getProperty("thresholds");
		}

		void trackOperation(String operation, long duration) {
			try {
				int movingAverageSize = (int) monitoringConfig.get("movingAverageSize");
				operationTimes.computeIfAbsent(operation,
						k -> new MovingAverage(movingAverageSize)).add(duration);
				analyzePerformance(operation);
			} catch (Exception e) {
				logger.error("Erreur lors du suivi de performance: {}", e.getMessage());
				recordFailure();
			}
		}

		private void analyzePerformance(String operation) {
			MovingAverage avg = operationTimes.get(operation);
			long checkTimeoutMs = (long) monitoringConfig.get("checkTimeoutMs");
			if (avg.getAverage() > checkTimeoutMs) {
				handleSlowOperation(operation, avg.getAverage());
			}
		}

		private void handleSlowOperation(String operation, double avgDuration) {
			int alertThreshold = (int) monitoringConfig.get("alertThreshold");
			if (consecutiveSlowOperations.incrementAndGet() >= alertThreshold) {
				systemMonitor.handleRiskySituation(
						String.format("Performance dégradée: %s (%.2fms)", operation, avgDuration));
				consecutiveSlowOperations.set(0);
			}
		}
	}

	private void analyzePerformanceTrends() {
		if (recentOperations.isEmpty())
			return;

		double avgTime = recentOperations.stream()
				.mapToLong(OperationMetric::getDuration)
				.average()
				.orElse(0.0);

		if (avgTime > WARNING_THRESHOLD) {
			systemMonitor.handleRiskySituation(
					String.format("Tendance performance dégradée: %.2fms", avgTime));
		}
	}

	/**
	 * Enregistre un événement critique et déclenche une alerte si le seuil est
	 * dépassé.
	 */
	void recordCriticalEvent() {
		criticalEvents.incrementAndGet();

		if (criticalEvents.get() > ERROR_THRESHOLD) {
			logger.warn("Seuil d'événements critiques dépassé: {}", criticalEvents.get());
			systemMonitor.handleRiskySituation("Seuil critique atteint");
		}
	}

	/**
	 * Enregistre le traitement d'une entrée et son temps d'exécution.
	 *
	 * @param startTime Timestamp de début du traitement en millisecondes
	 */
	void recordProcessing(long startTime) {
		processedEntries.incrementAndGet();
		totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);
	}

	/**
	 * Enregistre un échec de traitement.
	 */
	void recordFailure() {
		failedProcessings.incrementAndGet();
	}

	/**
	 * Réinitialise toutes les statistiques à zéro.
	 */
	void reset() {
		processedEntries.set(0);
		totalProcessingTime.set(0);
		failedProcessings.set(0);
		criticalEvents.set(0);
		logger.debug("Statistiques de performance réinitialisées");
	}

	/**
	 * Retourne les métriques de base du système.
	 *
	 * @return Map contenant les métriques de base (entrées traitées et temps moyen)
	 */
	Map<String, Number> getMetrics() {
		Map<String, Number> metrics = new HashMap<>();
		metrics.put("processedEntries", processedEntries.get());
		metrics.put("averageProcessingTime", getAverageProcessingTime());
		return metrics;
	}

	/**
	 * Retourne toutes les statistiques, y compris les événements critiques et le
	 * taux d'échec.
	 *
	 * @return Map contenant toutes les statistiques disponibles
	 */
	Map<String, Number> getStats() {
		Map<String, Number> stats = new HashMap<>(getMetrics());
		stats.put("criticalEvents", criticalEvents.get());
		stats.put("failureRate", getFailureRate());
		return stats;
	}

	/**
	 * Calcule le temps moyen de traitement en millisecondes.
	 *
	 * @return Temps moyen de traitement, ou 0 si aucune entrée n'a été traitée
	 */
	private double getAverageProcessingTime() {
		int count = processedEntries.get();
		return count > 0 ? (double) totalProcessingTime.get() / count : 0;
	}

	/**
	 * Calcule le taux d'échec des traitements.
	 *
	 * @return Taux d'échec entre 0.0 et 1.0
	 */
	private double getFailureRate() {
		int total = processedEntries.get();
		return total > 0 ? (double) failedProcessings.get() / total : 0;
	}
}
