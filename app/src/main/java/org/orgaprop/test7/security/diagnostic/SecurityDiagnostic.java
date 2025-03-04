package org.orgaprop.test7.security.diagnostic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.Getter;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.orgaprop.test7.security.diagnostic.ResourceHandle;
import org.orgaprop.test7.security.diagnostic.ResourceInfo;
import org.orgaprop.test7.security.diagnostic.SecurityIssue;
import org.orgaprop.test7.security.diagnostic.UnifiedSystemMonitoring;
import org.orgaprop.test7.security.diagnostic.ErrorManager;
import org.orgaprop.test7.security.diagnostic.EmergencyProtocol;
import org.orgaprop.test7.security.diagnostic.SecurityError;
import org.orgaprop.test7.security.diagnostic.ResourceManager;
import org.orgaprop.test7.security.diagnostic.DiagnosticResult;

public class SecurityDiagnostic implements AutoCloseable {
	private static final UnifiedSystemMonitoring systemMonitor = new UnifiedSystemMonitoring();
	private static final Logger logger = LoggerFactory.getLogger(SecurityDiagnostic.class);
	private static final int ERROR_THRESHOLD = 10;
	private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private static final int MAX_ERROR_HISTORY = 100;
	private static final Queue<SecurityError> errorHistory = new ConcurrentLinkedQueue<>();

	private static final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private static final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
	private static final Map<SecurityCategory, AtomicInteger> errorCounters = new ConcurrentHashMap<>();
	private static final int ERROR_RETENTION_HOURS = 24;

	private static final long CACHE_DURATION_MS = 300000; // 5 minutes
	private static volatile DiagnosticResult cachedResult;
	private static volatile long lastCheckTime;

	private static final long CHECK_TIMEOUT_MS = 5000; // 5 secondes
	private static final ExecutorService executorService = Executors.newCachedThreadPool();

	private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
	private static final Lock readLock = cacheLock.readLock();
	private static final Lock writeLock = cacheLock.writeLock();

	private static final ConcurrentHashMap<String, WeakReference<DiagnosticResult>> resultCache = new ConcurrentHashMap<>();
	private static final DiagnosticLogger diagnosticLogger = new DiagnosticLogger();

	public enum SecuritySeverity {
		CRITICAL("CRITIQUE"),
		WARNING("ATTENTION"),
		INFO("INFO");

		private final String label;

		SecuritySeverity(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	public enum IssuePriority {
		HIGH(1),
		MEDIUM(2),
		LOW(3);

		private final int value;

		IssuePriority(int value) {
			this.value = value;
		}
	}

	public enum SecurityCategory {
		CRYPTO("Cryptographie"),
		STORAGE("Stockage"),
		VALIDATION("Validation"),
		GENERAL("Général");

		private final String label;

		SecurityCategory(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

	public static DiagnosticResult performSecurityCheck() {
		if (!systemMonitor.isOperational()) {
			throw new SecurityException("Système non opérationnel");
		}

		List<SecurityIssue> issues = new ArrayList<>();
		try {
			checkCryptoConfig(issues);
			checkStorageConfig(issues);
			checkValidationConfig(issues);

			return new DiagnosticResult(issues.isEmpty(), issues);
		} catch (Exception e) {
			systemMonitor.handleMonitoringError(e);
			throw e;
		}
	}

	private static void checkCryptoConfig(List<SecurityIssue> issues) {
		if (SecurityConfig.Crypto.KEY_SIZE < SecurityThresholds.MIN_KEY_SIZE) {
			issues.add(new SecurityIssue(
					SecuritySeverity.CRITICAL,
					"Taille de clé insuffisante",
					"Augmenter la taille de clé à au moins " + SecurityThresholds.MIN_KEY_SIZE + " bits",
					IssuePriority.HIGH,
					SecurityCategory.CRYPTO));
		}
		for (String mode : SecurityThresholds.REQUIRED_CIPHER_MODES) {
			if (!SecurityConfig.Crypto.TRANSFORMATION.contains(mode)) {
				issues.add(new SecurityIssue(
						SecuritySeverity.CRITICAL,
						"Mode de chiffrement non sécurisé",
						"Utiliser un mode de chiffrement sécurisé tel que " + mode,
						IssuePriority.HIGH,
						SecurityCategory.CRYPTO));
			}
		}
	}

	private static void checkStorageConfig(List<SecurityIssue> issues) {
		if (!SecurityConfig.Storage.VERSION.equals("1")) {
			issues.add(new SecurityIssue(
					SecuritySeverity.CRITICAL,
					"Version de stockage non supportée",
					"Mettre à jour la version de stockage à une version supportée",
					IssuePriority.HIGH,
					SecurityCategory.STORAGE));
		}
	}

	private static void checkValidationConfig(List<SecurityIssue> issues) {
		if (SecurityConfig.Validation.MAX_ATTEMPTS > SecurityThresholds.MAX_FAILED_ATTEMPTS) {
			issues.add(new SecurityIssue(
					SecuritySeverity.CRITICAL,
					"Limite de tentatives trop élevée",
					"Réduire la limite à " + SecurityThresholds.MAX_FAILED_ATTEMPTS + " tentatives",
					IssuePriority.HIGH,
					SecurityCategory.VALIDATION));
		}
		if (SecurityConfig.Validation.SESSION_TIMEOUT > SecurityThresholds.MAX_SESSION_TIMEOUT) {
			issues.add(new SecurityIssue(
					SecuritySeverity.CRITICAL,
					"Timeout de session trop long",
					"Réduire le timeout de session à " + SecurityThresholds.MAX_SESSION_TIMEOUT + " millisecondes ou moins",
					IssuePriority.HIGH,
					SecurityCategory.VALIDATION));
		}
	}

	private static void checkDependencyCycles(List<ValidationDependency> dependencies) {
		Set<String> visited = new HashSet<>();
		Set<String> recursionStack = new HashSet<>();

		for (ValidationDependency dep : dependencies) {
			if (hasCycle(dep.ruleName, dependencies, visited, recursionStack)) {
				throw new IllegalStateException("Dépendance cyclique détectée pour: " + dep.ruleName);
			}
		}
	}

	private static boolean hasCycle(String ruleName,
			List<ValidationDependency> dependencies,
			Set<String> visited,
			Set<String> recursionStack) {
		if (recursionStack.contains(ruleName)) {
			return true;
		}
		if (visited.contains(ruleName)) {
			return false;
		}

		visited.add(ruleName);
		recursionStack.add(ruleName);

		for (ValidationDependency dep : dependencies) {
			if (dep.ruleName.equals(ruleName)) {
				for (String dependency : dep.dependencies) {
					if (hasCycle(dependency, dependencies, visited, recursionStack)) {
						return true;
					}
				}
			}
		}

		recursionStack.remove(ruleName);
		return false;
	}

	public static DiagnosticResult performSecurityCheckWithTimeout() throws TimeoutException {
		Future<DiagnosticResult> future = executorService.submit(() -> performSecurityCheck());
		try {
			return future.get(CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new TimeoutException("Vérification de sécurité interrompue : timeout");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SecurityException("Vérification de sécurité interrompue", e);
		} catch (Exception e) {
			throw new SecurityException("Erreur lors de la vérification", e);
		}
	}

	public static void shutdown() {
		if (!isShuttingDown.compareAndSet(false, true)) {
			return;
		}
		try {
			systemMonitor.close();
			cleanup();
			executorService.shutdown();
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			executorService.shutdownNow();
		} finally {
			isInitialized.set(false);
			isShuttingDown.set(false);
		}
	}

	private static DiagnosticResult getCachedOrCheck() {
		try (ResourceHandle readHandle = new ResourceHandle(readLock)) {
			if (isCacheValid()) {
				return cachedResult;
			}
		}

		try (ResourceHandle writeHandle = new ResourceHandle(writeLock)) {
			if (isCacheValid()) {
				return cachedResult;
			}
			cachedResult = performSecurityCheck();
			lastCheckTime = System.currentTimeMillis();
			return cachedResult;
		}
	}

	private static boolean isCacheValid() {
		return cachedResult != null &&
				(System.currentTimeMillis() - lastCheckTime) <= CACHE_DURATION_MS;
	}

	public static void cleanup() {
		// Nettoyage du cache
		resultCache.entrySet().removeIf(entry -> entry.getValue().get() == null);
		// Nettoyage des autres ressources
		executorService.shutdown();
		cachedResult = null;
	}

	private static void handleCriticalError(SecurityCategory category, Exception e) {
		SecurityError error = new SecurityError(category, e);
		systemMonitor.handleError(error);

		logger.error("Erreur critique: {} dans {}", e.getMessage(), category, e);
		if (systemMonitor.isSystemAtRisk()) {
			throw new SecurityException(error.message, e);
		}
	}

	public static void initialize() {
		if (!isInitialized.compareAndSet(false, true)) {
			throw new IllegalStateException("SecurityDiagnostic déjà initialisé");
		}
		validateThresholds();
		systemMonitor.startMonitoring();
		startBackgroundTasks();
	}

	private static void startBackgroundTasks() {
		executorService.submit(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					performSecurityCheckWithTimeout();
					Thread.sleep(CACHE_DURATION_MS);
				} catch (InterruptedException | TimeoutException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});
	}

	private static void notifySystemAdmin(SecurityIssue issue) {
		logger.warn("ALERTE ADMIN - Issue critique détectée: {}", issue.message);
	}

	private static void handleResourceCloseErrors(List<ResourceCloseError> errors) {
		StringBuilder errorMessage = new StringBuilder("Erreurs lors de la fermeture des ressources:\n");
		for (ResourceCloseError error : errors) {
			errorMessage.append(String.format(
					"- Ressource créée à %s: %s\n",
					error.resourceInfo.creationTrace,
					error.error.getMessage()));
		}
		logger.error(errorMessage.toString());
		throw new SecurityException(errorMessage.toString());
	}

	private static void triggerCriticalAlert(SecurityError error) {
		SecurityIssue issue = new SecurityIssue(
				SecuritySeverity.CRITICAL,
				"Alerte critique: " + error.message,
				"Vérifier les logs système immédiatement",
				IssuePriority.HIGH,
				error.category);
		notifySystemAdmin(issue);
	}

	@Override
	public void close() {
		try {
			shutdown();
		} catch (Exception e) {
			logger.error("Erreur lors de la fermeture", e);
			throw new SecurityException("Erreur lors de la fermeture", e);
		}
	}

	public interface IssueFormatter {
		String format(SecurityIssue issue);
	}

	public static class DefaultFormatter implements IssueFormatter {
		@Override
		public String format(SecurityIssue issue) {
			return String.format("[%s] %s - %s",
					issue.severity,
					issue.message,
					issue.recommendation);
		}
	}

	private static class ValidationDependency {
		private final String ruleName;
		private final Set<String> dependencies;

		public ValidationDependency(String ruleName, String... dependencies) {
			this.ruleName = ruleName;
			this.dependencies = new HashSet<>(Arrays.asList(dependencies));
		}
	}

	private static class InterruptibleSecurityCheck {
		private static final AtomicBoolean isCheckInProgress = new AtomicBoolean(false);

		public static DiagnosticResult performCheck() throws SecurityException {
			if (!isCheckInProgress.compareAndSet(false, true)) {
				throw new SecurityException("Une vérification est déjà en cours");
			}

			try {
				return performSecurityCheck();
			} finally {
				isCheckInProgress.set(false);
			}
		}
	}

	private static class MemoryMonitor {
		private static final Runtime runtime = Runtime.getRuntime();
		private static final double CRITICAL_THRESHOLD = 0.90; // 90%
		private static final double WARNING_THRESHOLD = 0.80; // 80%
		private static final AtomicInteger cleanupCount = new AtomicInteger(0);

		static void checkMemory() {
			long maxMemory = runtime.maxMemory();
			long usedMemory = runtime.totalMemory() - runtime.freeMemory();
			double memoryUsage = (double) usedMemory / maxMemory;

			if (memoryUsage >= CRITICAL_THRESHOLD) {
				int attempts = cleanupCount.incrementAndGet();
				logger.error("Mémoire CRITIQUE (tentative {}): {}/{} Mo ({:.1f}%)",
						attempts,
						usedMemory / 1048576,
						maxMemory / 1048576,
						memoryUsage * 100);

				if (attempts >= 3) {
					handleCriticalMemoryError();
				} else {
					emergencyCleanup();
				}
			} else if (memoryUsage >= WARNING_THRESHOLD) {
				logger.warn("Mémoire haute: {}/{} Mo ({:.1f}%)",
						usedMemory / 1048576,
						maxMemory / 1048576,
						memoryUsage * 100);
				cleanupCache();
			}
		}

		private static void emergencyCleanup() {
			cleanupCache();
			System.gc();
			logger.info("Nettoyage d'urgence effectué");
		}

		private static void handleCriticalMemoryError() {
			throw new OutOfMemoryError("Échec de la récupération de mémoire après plusieurs tentatives");
		}
	}

	private static class SecurityUtils {
		static boolean needsCleanup(ResourceUsage usage) {
			return System.currentTimeMillis() - usage.lastCleanupTime.get() > Duration.ofHours(1).toMillis();
		}

		static boolean isSystemOverloaded() {
			Runtime runtime = Runtime.getRuntime();
			double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) /
					runtime.maxMemory();
			return memoryUsage > MEMORY_THRESHOLD;
		}

		static Map<SecurityCategory, Integer> collectErrorCounts(
				Map<SecurityCategory, ErrorMetrics> metrics) {
			Map<SecurityCategory, Integer> counts = new ConcurrentHashMap<>();
			metrics.forEach((category, metric) -> counts.put(category, metric.criticalCount.get()));
			return counts;
		}
	}

	private static class ResourceCloseError {
		final ResourceInfo resourceInfo;
		final Exception error;

		ResourceCloseError(ResourceInfo resourceInfo, Exception error) {
			this.resourceInfo = resourceInfo;
			this.error = error;
		}
	}

}
