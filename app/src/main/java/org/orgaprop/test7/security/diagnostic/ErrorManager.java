package org.orgaprop.test7.security.diagnostic;

// Imports Lombok
import lombok.extern.slf4j.Slf4j;
import lombok.Data;

// Imports Java standard
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// Imports tiers
import org.apache.commons.collections4.queue.CircularFifoQueue;

// Imports internes
import org.orgaprop.test7.security.diagnostic.EmergencyProtocol;
import org.orgaprop.test7.security.diagnostic.SecurityError;
import org.orgaprop.test7.security.diagnostic.SecurityCategory;
import org.orgaprop.test7.security.diagnostic.SecuritySeverity;

/**
 * Gestionnaire centralisé des erreurs du système de sécurité.
 * Cette classe thread-safe gère le traitement, l'escalade et le suivi des
 * erreurs.
 * Elle maintient des statistiques par catégorie et déclenche des protocoles
 * d'urgence lorsque nécessaire.
 */
@Slf4j
public class ErrorManager {
	/** Nombre maximum d'erreurs consécutives avant escalade */
	private static final int MAX_CONSECUTIVE_ERRORS = 5;
	/** Fenêtre de temps pour l'analyse des erreurs */
	private static final Duration ERROR_WINDOW = Duration.ofMinutes(15);
	/** Seuil d'erreurs critiques déclenchant une escalade */
	private static final int CRITICAL_THRESHOLD = 3;
	/** Durée de conservation des erreurs dans l'historique */
	private static final Duration RETENTION_PERIOD = Duration.ofHours(24);
	/** Période de refroidissement entre deux escalades */
	private static final Duration COOLDOWN_PERIOD = Duration.ofMinutes(30);
	/** Taille maximale de la file d'erreurs */
	private static final int MAX_ERROR_QUEUE = 100;

	// États et métriques
	private static final Map<SecurityCategory, ErrorStats> errorStats = new ConcurrentHashMap<>();
	private static final Map<SecurityCategory, ErrorQueue> errorQueues = new ConcurrentHashMap<>();
	private static final Map<SecurityCategory, CircularFifoQueue<SecurityError>> errorHistory = new ConcurrentHashMap<>();
	private static final AtomicBoolean emergencyMode = new AtomicBoolean(false);

	/**
	 * Statistiques d'erreurs pour une catégorie de sécurité.
	 * Maintient les compteurs et l'historique des erreurs récentes.
	 */
	@lombok.Data
	public static class ErrorStats {
		private final AtomicInteger countTotal = new AtomicInteger();
		private final AtomicInteger countCritical = new AtomicInteger();
		private final CircularFifoQueue<SecurityError> recentErrors = new CircularFifoQueue<>(MAX_ERROR_QUEUE);
		private volatile LocalDateTime lastCritical;
		private final AtomicLong lastEmergencyTime = new AtomicLong(0);

		/**
		 * Enregistre une nouvelle erreur et met à jour les statistiques.
		 * 
		 * @param error L'erreur à enregistrer
		 */
		public synchronized void recordError(SecurityError error) {
			countTotal.incrementAndGet();
			recentErrors.add(error);

			if (error.severity == SecuritySeverity.CRITICAL) {
				handleCriticalError(error);
			}

			cleanupOldErrors();
			checkErrorThresholds();
		}

		/**
		 * Traite une erreur critique et déclenche l'escalade si nécessaire.
		 * 
		 * @param error L'erreur critique à traiter
		 */
		private void handleCriticalError(SecurityError error) {
			countCritical.incrementAndGet();
			lastCritical = LocalDateTime.now();

			if (hasReachedCriticalThreshold()) {
				escalateError(error);
			}
		}

		/**
		 * Vérifie si le système est en situation de risque pour cette catégorie.
		 * 
		 * @return true si le seuil critique est atteint
		 */
		public boolean isSystemAtRisk() {
			return countCritical.get() >= CRITICAL_THRESHOLD ||
					hasReachedCriticalThreshold();
		}
	}

	/**
	 * File d'attente des erreurs avec gestion du niveau de sévérité.
	 */
	private static class ErrorQueue {
		private final CircularFifoQueue<SecurityError> errors;
		private final AtomicInteger severityLevel;
		private final AtomicLong lastEmergencyTime;

		ErrorQueue() {
			this.errors = new CircularFifoQueue<>(MAX_ERROR_QUEUE);
			this.severityLevel = new AtomicInteger(0);
			this.lastEmergencyTime = new AtomicLong(0);
		}

		/**
		 * Traite une nouvelle erreur dans la file.
		 * 
		 * @param error L'erreur à traiter
		 */
		synchronized void processError(SecurityError error) {
			errors.add(error);
			updateSeverityLevel(error);
			cleanupOldErrors();
			checkErrorThresholds();
		}

		private boolean hasReachedCriticalThreshold() {
			if (emergencyMode.get())
				return false;

			LocalDateTime cutoff = LocalDateTime.now().minus(ERROR_WINDOW);
			long recentCriticalErrors = errors.stream()
					.filter(e -> e.severity == SecuritySeverity.CRITICAL)
					.filter(e -> e.timestamp.isAfter(cutoff))
					.count();

			return recentCriticalErrors >= CRITICAL_THRESHOLD;
		}
	}

	/**
	 * Traite une nouvelle erreur de sécurité.
	 * 
	 * @param error L'erreur à traiter
	 */
	public static void handleError(SecurityError error) {
		// Mise à jour des statistiques globales
		ErrorStats stats = errorStats.computeIfAbsent(
				error.category,
				k -> new ErrorStats());
		stats.recordError(error);

		// Traitement dans la file d'erreurs
		ErrorQueue queue = errorQueues.computeIfAbsent(
				error.category,
				k -> new ErrorQueue());
		queue.processError(error);

		// Vérification des seuils critiques
		if (shouldEscalate(error)) {
			escalateError(error);
		}

		updateErrorHistory(error);
	}

	/**
	 * Récupère les statistiques actuelles des erreurs par catégorie.
	 * 
	 * @return Une copie des statistiques courantes
	 */
	public static Map<SecurityCategory, ErrorStats> getCurrentStats() {
		return new ConcurrentHashMap<>(errorStats);
	}

	/**
	 * Déclenche une escalade d'erreur critique.
	 * 
	 * @param error L'erreur ayant déclenché l'escalade
	 */
	private static void escalateError(SecurityError error) {
		log.error("Escalade d'erreur critique: {}", error.message);
		if (isSystemwideEmergency()) {
			initiateEmergencyShutdown("Erreurs critiques multiples");
		}
	}

	/**
	 * Vérifie si le système est en situation d'urgence générale.
	 * 
	 * @return true si trop de catégories sont en situation critique
	 */
	private static boolean isSystemwideEmergency() {
		return errorStats.values().stream()
				.filter(ErrorStats::isSystemAtRisk)
				.count() >= 3;
	}

	/**
	 * Initie un arrêt d'urgence du système.
	 * 
	 * @param reason La raison de l'arrêt d'urgence
	 */
	private static void initiateEmergencyShutdown(String reason) {
		if (emergencyMode.compareAndSet(false, true)) {
			EmergencyProtocol.execute(reason);
		}
	}

	/**
	 * Nettoie toutes les files d'erreurs et réinitialise l'état d'urgence.
	 */
	public static void cleanup() {
		errorQueues.clear();
		errorStats.clear();
		errorHistory.clear();
		emergencyMode.set(false);
	}
}
