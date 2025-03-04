package org.orgaprop.test7.security.diagnostic;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicBoolean;

import org.orgaprop.test7.security.diagnostic.DiagnosticResult;

/**
 * Gestionnaire de cache pour les résultats de diagnostics de sécurité.
 * Utilise des références faibles (SoftReference) pour permettre la libération
 * de mémoire si nécessaire par le GC.
 */
@Slf4j
public class CacheManager implements AutoCloseable {
	// Constantes de configuration
	/** Intervalle entre deux nettoyages programmés du cache */
	private static final Duration CLEANUP_INTERVAL = Duration.ofHours(6);
	/** Durée de validité d'une entrée dans le cache */
	private static final Duration CACHE_DURATION = Duration.ofMinutes(5);
	/** Seuil d'utilisation mémoire déclenchant un nettoyage d'urgence */
	private static final double MEMORY_THRESHOLD = 0.85;

	/** Cache principal stockant les résultats de diagnostic */
	private static final Map<String, SoftReference<DiagnosticResult>> resultCache = new ConcurrentHashMap<>();

	/** Exécuteur pour les tâches de nettoyage programmées */
	private static final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "Cache-Cleanup-Thread");
		t.setDaemon(true);
		return t;
	});

	/** Indicateur d'arrêt du gestionnaire */
	private static final AtomicBoolean isShutdown = new AtomicBoolean(false);

	/**
	 * Démarre la tâche périodique de nettoyage du cache.
	 * Cette méthode doit être appelée une seule fois au démarrage.
	 */
	static void startCleanupTask() {
		cleanupScheduler.scheduleAtFixedRate(() -> {
			try {
				cleanupCache();
			} catch (Exception e) {
				log.error("Erreur lors du nettoyage du cache", e);
			}
		}, 1, CLEANUP_INTERVAL.toHours(), TimeUnit.HOURS);
	}

	/**
	 * Nettoie le cache en supprimant les références nulles ou périmées.
	 */
	private static void cleanupCache() {
		log.debug("Début du nettoyage du cache");
		int initialSize = resultCache.size();

		resultCache.entrySet().removeIf(entry -> {
			SoftReference<DiagnosticResult> ref = entry.getValue();
			boolean shouldRemove = ref == null || ref.get() == null;
			if (shouldRemove) {
				log.debug("Suppression de l'entrée du cache: {}", entry.getKey());
			}
			return shouldRemove;
		});

		log.debug("Nettoyage terminé: {} entrées supprimées",
				initialSize - resultCache.size());

		checkMemoryUsage();
	}

	private static void checkMemoryUsage() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		double memoryUsage = (double) usedMemory / maxMemory;

		if (memoryUsage > MEMORY_THRESHOLD) {
			log.warn("Usage mémoire élevé après nettoyage: {:.1f}%", memoryUsage * 100);
			emergencyCleanup();
		}
	}

	private static void emergencyCleanup() {
		log.info("Démarrage nettoyage d'urgence du cache");
		resultCache.clear();
		System.gc();
		log.info("Nettoyage d'urgence terminé");
	}

	static void shutdown() {
		if (isShutdown.compareAndSet(false, true)) {
			log.info("Arrêt du gestionnaire de cache");
			cleanupScheduler.shutdown();
			try {
				if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					cleanupScheduler.shutdownNow();
					log.warn("Forçage de l'arrêt du nettoyeur de cache");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				cleanupScheduler.shutdownNow();
			} finally {
				cleanupCache();
			}
		}
	}

	@Override
	public void close() {
		shutdown();
	}

	// Méthodes utilitaires
	static DiagnosticResult getCachedResult(String key) {
		SoftReference<DiagnosticResult> ref = resultCache.get(key);
		return ref != null ? ref.get() : null;
	}

	static void cacheResult(String key, DiagnosticResult result) {
		resultCache.put(key, new SoftReference<>(result));
	}

	// Hook d'arrêt
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				shutdown();
			} catch (Exception e) {
				log.error("Erreur lors de l'arrêt du cache", e);
			}
		}));
	}
}
