package org.orgaprop.test7.security.diagnostic;

// Imports Java standard
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// Imports de bibliothèques tierces
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Imports internes au projet
import org.orgaprop.test7.security.diagnostic.EmergencyProtocol;
import org.orgaprop.test7.security.diagnostic.ErrorManager;
import org.orgaprop.test7.security.diagnostic.ResourceManager;
import org.orgaprop.test7.security.diagnostic.monitoring.ErrorStats;
import org.orgaprop.test7.security.diagnostic.security.SecurityCategory;

/**
 * Système unifié de monitoring combinant la surveillance système, ressources et
 * sécurité.
 */
public class UnifiedSystemMonitoring implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(UnifiedSystemMonitoring.class);

	// Instance unique
	private static volatile UnifiedSystemMonitoring instance;

	// Verrou pour double-checked locking
	private static final Object LOCK = new Object();

	// Constructeur privé
	private UnifiedSystemMonitoring() {
		if (instance != null) {
			throw new IllegalStateException("Instance déjà créée - Utiliser getInstance()");
		}
	}

	/**
	 * Retourne l'instance unique de UnifiedSystemMonitoring.
	 * Utilise le pattern double-checked locking pour la thread-safety.
	 *
	 * @return L'instance unique de UnifiedSystemMonitoring
	 */
	public static UnifiedSystemMonitoring getInstance() {
		UnifiedSystemMonitoring result = instance;

		if (result == null) {
			synchronized (LOCK) {
				result = instance;
				if (result == null) {
					instance = result = new UnifiedSystemMonitoring();
				}
			}
		}

		return result;
	}

	// Constantes de configuration
	private static final Duration CHECK_INTERVAL = Duration.ofSeconds(1);
	private static final double MEMORY_THRESHOLD = 0.85;
	private static final int MAX_RESOURCES = 1000;
	private static final int THREAD_THRESHOLD = 100;
	private static final int ERROR_THRESHOLD = 10;
	private static final Duration GRACE_PERIOD = Duration.ofSeconds(30);

	// Gestionnaire d'exécution
	private final ExecutorService monitorExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Unified-Monitor");
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		return t;
	});

	// État du système
	private final AtomicReference<SystemState> systemState = new AtomicReference<>(SystemState.RUNNING);
	private final CircularFifoQueue<SystemMetrics> metricsHistory = new CircularFifoQueue<>(100);

	/**
	 * Démarre la surveillance du système.
	 * Lance un thread dédié qui surveille périodiquement les métriques système.
	 */
	public void startMonitoring() {
		monitorExecutor.submit(this::monitoringLoop);
	}

	/**
	 * Boucle principale de surveillance qui collecte et analyse les métriques
	 * système.
	 * S'exécute jusqu'à interruption ou arrêt du système.
	 */
	private void monitoringLoop() {
		while (!Thread.currentThread().isInterrupted() && isOperational()) {
			try {
				SystemMetrics metrics = collectSystemMetrics();
				metricsHistory.add(metrics);

				if (metrics.isSystemAtRisk()) {
					handleRiskySituation(metrics);
				}

				Thread.sleep(CHECK_INTERVAL.toMillis());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				handleMonitoringError(e);
			}
		}
	}

	/**
	 * Collecte les métriques système actuelles.
	 *
	 * @return SystemMetrics contenant les métriques collectées
	 */
	private SystemMetrics collectSystemMetrics() {
		Runtime runtime = Runtime.getRuntime();
		return SystemMetrics.builder()
				.memoryUsage(calculateMemoryUsage(runtime))
				// Utiliser les méthodes statiques des gestionnaires dédiés
				.resourceCount(ResourceManager.getResourceCount())
				.activeThreads(Thread.activeCount())
				.errorStats(ErrorManager.getCurrentStats())
				.timestamp(LocalDateTime.now())
				.build();
	}

	/**
	 * Gère une situation à risque détectée dans les métriques système.
	 *
	 * @param metrics Les métriques système présentant un risque
	 */
	private void handleRiskySituation(SystemMetrics metrics) {
		logger.warn("Situation à risque détectée: {}", metrics);
		if (metrics.isSystemAtRisk()) {
			initiateEmergencyProtocol("Métriques système critiques");
		}
	}

	/**
	 * Gère une erreur survenue pendant la surveillance.
	 *
	 * @param e L'exception survenue pendant la surveillance
	 */
	private void handleMonitoringError(Exception e) {
		logger.error("Erreur de monitoring", e);
		if (isErrorCritical(e)) {
			initiateEmergencyProtocol("Erreur critique de monitoring");
		}
	}

	/**
	 * Initie le protocole d'urgence du système.
	 *
	 * @param reason La raison du déclenchement du protocole d'urgence
	 */
	private void initiateEmergencyProtocol(String reason) {
		if (systemState.compareAndSet(SystemState.RUNNING, SystemState.EMERGENCY)) {
			logger.error("PROTOCOLE D'URGENCE INITIÉ: {}", reason);
			EmergencyProtocol.execute(this, reason);
		}
	}

	@Override
	public void close() {
		monitorExecutor.shutdownNow();
		try {
			if (!monitorExecutor.awaitTermination(GRACE_PERIOD.toSeconds(), TimeUnit.SECONDS)) {
				logger.warn("Arrêt forcé du monitoring");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Interruption pendant l'arrêt du monitoring");
		}
	}

	/**
	 * Vérifie si le système est opérationnel.
	 *
	 * @return true si le système est en état de fonctionnement normal ou
	 *         d'avertissement
	 */
	private boolean isOperational() {
		return systemState.get() == SystemState.RUNNING ||
				systemState.get() == SystemState.WARNING;
	}

	/**
	 * Calcule le pourcentage d'utilisation de la mémoire.
	 *
	 * @param runtime L'instance Runtime pour accéder aux informations mémoire
	 * @return Le pourcentage d'utilisation de la mémoire (entre 0.0 et 1.0)
	 */
	private static double calculateMemoryUsage(Runtime runtime) {
		return (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
	}

	@Builder
	@Data
	public static class SystemMetrics {
		/** Pourcentage d'utilisation de la mémoire (entre 0.0 et 1.0) */
		private final double memoryUsage;

		/** Nombre de ressources système actuellement utilisées */
		private final int resourceCount;

		/** Nombre de threads actifs */
		private final int activeThreads;

		/** Statistiques d'erreurs par catégorie de sécurité */
		private final Map<SecurityCategory, ErrorStats> errorStats;

		/** Horodatage de la collecte des métriques */
		private final LocalDateTime timestamp;

		/**
		 * Vérifie si le système est dans un état à risque.
		 *
		 * @return true si au moins une métrique dépasse son seuil critique
		 */
		public boolean isSystemAtRisk() {
			return memoryUsage >= MEMORY_THRESHOLD ||
					resourceCount >= MAX_RESOURCES ||
					activeThreads > THREAD_THRESHOLD ||
					hasExcessiveErrors();
		}

		/**
		 * Vérifie si le nombre d'erreurs dépasse le seuil critique.
		 *
		 * @return true si le nombre d'erreurs est excessif
		 */
		private boolean hasExcessiveErrors() {
			return errorStats.values().stream()
					.anyMatch(ErrorStats::isCritical);
		}
	}

	@Getter
	public enum SystemState {
		RUNNING("En cours"),
		WARNING("Avertissement"),
		CRITICAL("Critique"),
		EMERGENCY("Urgence"),
		SHUTDOWN("Arrêt");

		private final String label;

		SystemState(String label) {
			this.label = label;
		}
	}

}
