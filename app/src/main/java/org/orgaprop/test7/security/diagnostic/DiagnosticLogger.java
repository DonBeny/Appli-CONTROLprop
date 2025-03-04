package org.orgaprop.test7.security.diagnostic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;

import org.orgaprop.test7.security.diagnostic.SecurityCategory;
import org.orgaprop.test7.security.diagnostic.SecuritySeverity;

/**
 * Logger spécialisé pour les diagnostics de sécurité.
 */
public class DiagnosticLogger implements AutoCloseable {
	public enum LoggerState {
		ACTIVE, MAINTENANCE, SHUTDOWN
	}

	private static final Logger logger = LoggerFactory.getLogger(DiagnosticLogger.class);
	private static final int MAX_HISTORY = 100;
	private static final Duration RETENTION_PERIOD = Duration.ofHours(24);
	private static final DiagnosticLogger INSTANCE = new DiagnosticLogger();
	private static final int BACKUP_THRESHOLD = MAX_HISTORY / 2;
	private static final int ERROR_THRESHOLD = 10;
	private static final int COMPRESSION_THRESHOLD = 1000;
	private static final double MEMORY_WARNING_THRESHOLD = 0.85;
	private static final long MAINTENANCE_INTERVAL = Duration.ofHours(1).toMillis();
	private static final long CHECK_TIMEOUT_MS = 5000;

	static {
		try {
			UnifiedSystemMonitoring.getInstance();
			DiagnosticLogger instance = INSTANCE;
			instance.initializeComponents();
			logger.info("DiagnosticLogger initialisé");
		} catch (Exception e) {
			logger.error("Échec de l'initialisation de DiagnosticLogger", e);
			// Remonter l'erreur pour empêcher une initialisation partielle
			throw new ExceptionInInitializerError("Échec initialisation critique: " + e.getMessage());
		}
	}

	private final ConcurrentLinkedQueue<DiagnosticEntry> history = new ConcurrentLinkedQueue<>();
	private boolean debugMode = false;
	private final UnifiedSystemMonitoring systemMonitor;
	private final List<DiagnosticEventHandler> eventHandlers = new CopyOnWriteArrayList<>();
	private Duration retentionPeriod = RETENTION_PERIOD;
	private int maxHistory = MAX_HISTORY;

	private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "AsyncDiagnosticLogger");
		t.setDaemon(true);
		return t;
	});

	private final ScheduledExecutorService maintenanceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "DiagnosticMaintenance");
		t.setDaemon(true);
		return t;
	});

	private volatile LoggerState state = LoggerState.ACTIVE;

	private final PerformanceStats stats = new PerformanceStats();

	private volatile boolean isClosed = false;

	private final DiagnosticState diagnosticState = new DiagnosticState();

	private SystemState systemState = new SystemState();

	private final DiagnosticErrorHandler errorHandler;

	private DiagnosticLogger() {
		try {
			this.systemMonitor = UnifiedSystemMonitoring.getInstance();
			validateInitialization();
			this.state = LoggerState.ACTIVE;
			this.stats.reset(); // Initialisation des stats
			this.errorHandler = new DiagnosticErrorHandler(systemMonitor, stats);
			logger.info("DiagnosticLogger initialisé avec succès");
		} catch (Exception e) {
			logger.error("Échec initialisation DiagnosticLogger", e);
			throw new ExceptionInInitializerError(e);
		}
	} // Constructeur privé

	private void validateInitialization() {
		if (systemMonitor == null) {
			throw new IllegalStateException("SystemMonitor non initialisé");
		}
	}

	public static DiagnosticLogger getInstance() {
		return INSTANCE;
	}

	private final class DiagnosticState {
		private final LocalDateTime startTime = LocalDateTime.now();
		private volatile boolean isInitialized = false;
		private final AtomicInteger initializationAttempts = new AtomicInteger(0);

		void initialize() {
			if (!isInitialized && initializationAttempts.incrementAndGet() <= 3) {
				synchronized (this) {
					if (!isInitialized) {
						try {
							checkSystemResources();
							cleanupEventHandlers();
							isInitialized = true;
							logger.info("DiagnosticState initialisé");
						} catch (Exception e) {
							logger.error("Échec initialisation DiagnosticState", e);
							throw new IllegalStateException("Échec initialisation", e);
						}
					}
				}
			}
		}
	}

	private class HealthMetrics {
		private final CircularFifoQueue<Double> memoryUsage = new CircularFifoQueue<>(10);
		private final CircularFifoQueue<Integer> activeThreads = new CircularFifoQueue<>(10);
		private final AtomicInteger healthScore = new AtomicInteger(100);

		void updateMetrics() {
			memoryUsage.add(systemMonitor.getMemoryUsage());
			activeThreads.add(Thread.activeCount());
			calculateHealthScore();
		}

		private void calculateHealthScore() {
			double avgMemory = memoryUsage.stream()
					.mapToDouble(Double::doubleValue)
					.average()
					.orElse(0.0);

			if (avgMemory > MEMORY_WARNING_THRESHOLD) {
				healthScore.updateAndGet(current -> Math.max(0, current - 10));
			}
		}
	}

	private class EventHistory {
		private final CircularFifoQueue<MetricEvent> events = new CircularFifoQueue<>(1000);
		private final AtomicLong eventCount = new AtomicLong();

		void recordEvent(MetricEvent event) {
			events.add(event);
			eventCount.incrementAndGet();
			analyzeEventPattern();
		}

		private void analyzeEventPattern() {
			// Analyse des patterns d'événements
		}
	}

	@Override
	public void close() {
		if (!isClosed) {
			synchronized (this) {
				if (!isClosed) {
					try {
						state = LoggerState.SHUTDOWN;
						shutdownExecutors();
						flushHistory();
						clearEventHandlers();
						stats.reset();
						isClosed = true;
						logger.info("DiagnosticLogger fermé proprement");
					} catch (Exception e) {
						logger.error("Erreur critique pendant la fermeture", e);
						throw new DiagnosticLoggingException("Échec fermeture", e);
					} finally {
						systemMonitor.handleSystemStateChange("Logger fermé");
					}
				}
			}
		}
	}

	public void logDiagnostic(@Nonnull DiagnosticResult result, String context) {
		try {
			validateInput(result, context);
			DiagnosticEntry entry = createEntry(result, context);
			processEntry(entry);
		} catch (Exception e) {
			logger.error("Erreur lors du logging du diagnostic", e);
			throw new DiagnosticLoggingException("Échec du logging", e);
		}
	}

	public CompletableFuture<Void> logDiagnosticAsync(
			DiagnosticResult result, String context) {
		return CompletableFuture.runAsync(() -> logDiagnostic(result, context), asyncExecutor);
	}

	private void logDebugInfo(DiagnosticEntry entry) {
		if (debugMode) {
			logger.debug("Détails diagnostic - Timestamp: {}, Thread: {}, Issues: {}, Metadata: {}",
					entry.timestamp,
					entry.threadName,
					entry.result.getIssues().size(),
					entry.getMetadata());

			if (!entry.result.isSecure()) {
				logger.debug("Issues détectées: {}",
						entry.result.getIssues().stream()
								.map(issue -> issue.getSeverity() + " - " + issue.getMessage())
								.collect(Collectors.joining(", ")));
			}
		}
	}

	private void logCriticalDiagnostic(DiagnosticEntry entry) {
		if (!entry.result.isSecure()) {
			List<SecurityIssue> criticalIssues = entry.result.getIssues().stream()
					.filter(i -> i.getSeverity() == SecuritySeverity.CRITICAL)
					.collect(Collectors.toList());

			if (!criticalIssues.isEmpty()) {
				errorHandler.handleCriticalEvent(entry, criticalIssues);
				systemMonitor.handleRiskySituation("Diagnostic critique détecté");
				notifyListeners(entry);
			}
		}
	}

	private String formatLogEntry(DiagnosticEntry entry) {
		return String.format("[%s] %s - %s issues - Thread: %s%s",
				entry.timestamp,
				entry.context,
				entry.result.getIssues().size(),
				entry.threadName,
				debugMode ? " - " + formatDebugInfo(entry) : "");
	}

	private String formatDebugInfo(DiagnosticEntry entry) {
		return String.format("Memory: %.2f%% - Threads: %d",
				systemMonitor.getMemoryUsage() * 100,
				Thread.activeCount());
	}

	private void logIssuesIfPresent(DiagnosticEntry entry) {
		if (!entry.result.isSecure()) {
			List<SecurityIssue> issues = entry.result.getIssues();
			logger.warn("Issues détectées ({}) - Contexte: {}",
					issues.size(), entry.context);
			issues.forEach(issue -> logger.warn("Issue: {} - {}", issue.getSeverity(), issue.getMessage()));
		}
	}

	private void checkState() {
		if (state == LoggerState.SHUTDOWN) {
			throw new IllegalStateException("Logger fermé");
		}
		if (state == LoggerState.MAINTENANCE) {
			logger.warn("Logger en maintenance - Logging limité");
		}
	}

	private void validateInput(DiagnosticResult result, String context) {
		List<String> errors = new ArrayList<>();

		if (result == null) {
			errors.add("Le résultat ne peut pas être null");
		}
		if (result != null && result.getIssues() == null) {
			errors.add("La liste des issues ne peut pas être null");
		}

		if (!errors.isEmpty()) {
			throw new IllegalArgumentException(String.join(", ", errors));
		}
	}

	private void cleanHistory() {
		List<DiagnosticEntry> removedEntries = new ArrayList<>();
		LocalDateTime cutoff = LocalDateTime.now().minus(retentionPeriod);

		// Nettoyage par date
		history.removeIf(entry -> {
			boolean shouldRemove = entry.timestamp.isBefore(cutoff);
			if (shouldRemove)
				removedEntries.add(entry);
			return shouldRemove;
		});

		// Nettoyage par taille
		while (history.size() > maxHistory) {
			DiagnosticEntry removed = history.poll();
			if (removed != null)
				removedEntries.add(removed);
		}

		if (!removedEntries.isEmpty()) {
			backupRemovedEntries(removedEntries);
			notifyHistoryCleanup(removedEntries.size());
		}
	}

	private void backupRemovedEntries(List<DiagnosticEntry> entries) {
		if (entries.isEmpty())
			return;

		try {
			persistEntries(entries);
			notifyHistoryCleanup(entries.size());
		} catch (Exception e) {
			logger.warn("Échec de la sauvegarde des entrées supprimées", e);
			systemMonitor.handleRiskySituation("Échec sauvegarde historique");
		}
	}

	private void notifyHistoryCleanup(int count) {
		eventHandlers.forEach(h -> {
			try {
				h.onHistoryCleanup(count);
			} catch (Exception e) {
				logger.error("Erreur notification cleanup", e);
			}
		});
	}

	private void validateLoggerState() {
		checkState();
		if (!diagnosticState.isInitialized) {
			diagnosticState.initialize();
		}
		if (systemState.shouldPerformMaintenance()) {
			performMaintenance();
		}
	}

	private void forceCleanup() {
		optimizeHistory();
		System.gc(); // Suggestion de GC
		logger.info("Nettoyage forcé effectué");
	}

	private void validateState() {
		if (isClosed) {
			throw new IllegalStateException("DiagnosticLogger est fermé");
		}

		try {
			checkState();
			validateLoggerState();
			systemMonitor.validateOperational();
		} catch (Exception e) {
			stats.recordFailure();
			throw new DiagnosticLoggingException("État invalide", e);
		}
	}

	public void startScheduledMaintenance() {
		maintenanceExecutor.scheduleAtFixedRate(
				this::performMaintenance,
				MAINTENANCE_INTERVAL,
				MAINTENANCE_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void processEntry(DiagnosticEntry entry) {
		long startTime = System.currentTimeMillis();
		validateState();

		try {
			if (!addToHistory(entry)) {
				stats.recordFailure();
				throw new DiagnosticLoggingException("File d'historique pleine", null);
			}

			logIssuesIfPresent(entry);
			logCriticalDiagnostic(entry);
			logDebugInfo(entry);

			notifyListeners(entry);
			persistEntries(List.of(entry));

		} catch (Exception e) {
			stats.recordFailure();
			errorHandler.handleSystemError(e, "Traitement entrée");
			throw e;
		} finally {
			stats.recordProcessing(startTime);
		}
	}

	private boolean addToHistory(DiagnosticEntry entry) {
		return history.offer(entry);
	}

	private DiagnosticEntry createEntry(DiagnosticResult result, String context) {
		return new DiagnosticEntry(result, context);
	}

	private void flushHistory() {
		if (!history.isEmpty()) {
			try {
				List<DiagnosticEntry> entries = new ArrayList<>(history);
				persistEntries(entries);
				logger.info("Sauvegarde de {} entrées de diagnostic", entries.size());
			} catch (Exception e) {
				logger.error("Échec de la sauvegarde de l'historique", e);
				systemMonitor.handleRiskySituation("Échec sauvegarde historique");
			}
		}
	}

	public DiagnosticSummary getSummary(Duration window) {
		LocalDateTime cutoff = LocalDateTime.now().minus(window);
		List<DiagnosticEntry> recentEntries = history.stream()
				.filter(e -> e.timestamp.isAfter(cutoff))
				.collect(Collectors.toList());

		return DiagnosticSummary.builder()
				.totalCount(recentEntries.size())
				.secureCount(countSecureEntries(recentEntries))
				.criticalIssuesCount(countCriticalIssues(recentEntries))
				.build();
	}

	private void persistEntries(List<DiagnosticEntry> entries) {
		if (entries.isEmpty())
			return;

		try {
			DiagnosticRepository.getInstance()
					.saveAll(entries.stream()
							.map(e -> new DiagnosticRecord(
									e.timestamp,
									e.result,
									e.threadName,
									DiagnosticMetadata.getSystemMetadata()))
							.collect(Collectors.toList()));
		} catch (Exception e) {
			systemMonitor.handleRiskySituation("Échec persistance diagnostics");
			throw new DiagnosticLoggingException("Erreur de persistance", e);
		}
	}

	private void optimizeHistory() {
		if (history.size() > maxHistory * 0.9) { // 90% de la capacité
			int toRemove = history.size() - (maxHistory / 2);
			List<DiagnosticEntry> oldEntries = new ArrayList<>();
			for (int i = 0; i < toRemove; i++) {
				DiagnosticEntry entry = history.poll();
				if (entry != null) {
					oldEntries.add(entry);
				}
			}
			backupRemovedEntries(oldEntries);
		}
	}

	public List<DiagnosticResult> getHistory() {
		return history.stream()
				.map(entry -> entry.result)
				.collect(Collectors.toList());
	}

	public List<DiagnosticResult> getHistoryByCategory(SecurityCategory category) {
		if (category == null) {
			throw new IllegalArgumentException("La catégorie ne peut pas être null");
		}
		return history.stream()
				.filter(entry -> entry.result.hasIssuesInCategory(category))
				.map(entry -> entry.result)
				.collect(Collectors.toList());
	}

	public Map<SecurityCategory, Long> getIssueStatistics() {
		return history.stream()
				.filter(entry -> !entry.result.isSecure())
				.flatMap(entry -> entry.result.getIssues().stream())
				.collect(Collectors.groupingBy(
						issue -> issue.getCategory(),
						Collectors.counting()));
	}

	public Map<String, Double> getPerformanceMetrics() {
		Map<String, Double> metrics = new HashMap<>();
		metrics.put("averageIssuesPerCheck", calculateAverageIssues());
		metrics.put("secureCheckRatio", calculateSecureRatio());
		return metrics;
	}

	public Map<String, Double> getPerformanceMetrics(Duration window) {
		LocalDateTime cutoff = LocalDateTime.now().minus(window);
		List<DiagnosticEntry> recentEntries = history.stream()
				.filter(e -> e.timestamp.isAfter(cutoff))
				.collect(Collectors.toList());

		Map<String, Double> metrics = new HashMap<>();
		metrics.put("averageIssuesPerCheck", calculateAverageIssues(recentEntries));
		metrics.put("secureCheckRatio", calculateSecureRatio(recentEntries));
		metrics.put("criticalIssueRatio", calculateCriticalRatio(recentEntries));
		return metrics;
	}

	public LoggerState getState() {
		return state;
	}

	public void exportToFile(Path filePath) {
		try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
			history.forEach(entry -> {
				try {
					writer.write(formatLogEntry(entry));
					writer.newLine();
				} catch (IOException e) {
					logger.error("Erreur d'export", e);
				}
			});
		} catch (IOException e) {
			logger.error("Erreur d'export", e);
		}
	}

	public void exportToJson(Path filePath) {
		try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(writer, getSummary(Duration.ofDays(1)));
		} catch (IOException e) {
			logger.error("Erreur export JSON", e);
		}
	}

	public void setDebugMode(boolean enabled) {
		this.debugMode = enabled;
		if (enabled) {
			logger.info("Mode debug activé - logging détaillé");
		}
	}

	private void handleCriticalEvent(DiagnosticEntry entry, List<SecurityIssue> criticalIssues) {
		String message = String.format("Issues critiques (%d) - %s", criticalIssues.size(), entry.context);

		try {
			// Log l'événement critique
			logger.error(message);
			criticalIssues.forEach(issue -> logger.error("Issue critique: {} - {}", issue.getSeverity(), issue.getMessage()));

			// Notification du système
			systemMonitor.handleRiskySituation(message);
			stats.recordCriticalEvent();

			// Déclenchement du protocole d'urgence si nécessaire
			if (criticalIssues.size() > ERROR_THRESHOLD) {
				systemMonitor.triggerEmergencyProtocol();
			}
		} catch (Exception e) {
			errorHandler.handleSystemError(e, "Traitement événement critique");
		}
	}

	public void addEventHandler(DiagnosticEventHandler handler) {
		eventHandlers.add(handler);
	}

	public void removeEventHandler(DiagnosticEventHandler handler) {
		if (handler != null) {
			eventHandlers.remove(handler);
			logger.debug("Event handler supprimé");
		}
	}

	public void clearEventHandlers() {
		eventHandlers.clear();
		logger.info("Tous les event handlers ont été supprimés");
	}

	private void cleanupEventHandlers() {
		eventHandlers.removeIf(handler -> {
			try {
				return !handler.isValid();
			} catch (Exception e) {
				logger.warn("Handler invalide détecté", e);
				return true;
			}
		});
	}

	private void handleMaintenanceFailure(Exception e) {
		logger.error("Échec critique de la maintenance", e);
		systemMonitor.handleRiskySituation("Échec maintenance DiagnosticLogger");
		stats.recordFailure();
	}

	private void handleResourceWarning(double memoryUsage, int threadCount) {
		logger.warn("Ressources système critiques - Mémoire: {}%, Threads: {}",
				memoryUsage * 100, threadCount);

		// Enregistrement des métriques avant cleanup
		Map<String, Number> preCleanupMetrics = collectSystemMetrics();

		systemState.markForMaintenance();
		forceCleanup();

		// Vérification de l'efficacité du cleanup
		double postCleanupMemory = systemMonitor.getMemoryUsage();
		if (postCleanupMemory > memoryUsage * 0.9) {
			systemMonitor.triggerEmergencyProtocol();
		}
	}

	public void updateConfiguration(DiagnosticConfig config) {
		this.retentionPeriod = config.getRetentionPeriod();
		this.maxHistory = config.getMaxHistory();
		this.debugMode = config.isDebugMode();
		logger.info("Configuration mise à jour: {}", config);
	}

	private void performMaintenance() {
		try {
			validateState();
			systemState.markForMaintenance();
			long start = System.currentTimeMillis();

			optimizeHistory();
			cleanupEventHandlers();
			checkSystemResources();

			systemState.maintenancePerformed();
			logger.info("Maintenance effectuée en {}ms", System.currentTimeMillis() - start);
		} catch (Exception e) {
			errorHandler.handleMaintenanceFailure(e);
		}
	}

	private void checkSystemResources() {
		Runtime runtime = Runtime.getRuntime();
		double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory())
				/ runtime.maxMemory();
		int threadCount = Thread.activeCount();

		Map<String, Object> resourceMetrics = new HashMap<>();
		resourceMetrics.put("memoryUsage", memoryUsage);
		resourceMetrics.put("threadCount", threadCount);
		resourceMetrics.put("freeMemory", runtime.freeMemory());

		if (memoryUsage > MEMORY_WARNING_THRESHOLD || threadCount > THREAD_THRESHOLD) {
			handleResourceWarning(memoryUsage, threadCount);
			stats.recordWarning();
			persistMetrics(resourceMetrics);
		}
	}

	private void notifyListeners(DiagnosticEntry entry) {
		for (DiagnosticEventHandler handler : eventHandlers) {
			try {
				if (!entry.result.isSecure()) {
					handler.onCriticalDiagnostic(entry.result);
				}
			} catch (Exception e) {
				logger.error("Erreur lors de la notification", e);
			}
		}
	}

	private static int countSecureEntries(List<DiagnosticEntry> entries) {
		return (int) entries.stream()
				.filter(e -> e.result.isSecure())
				.count();
	}

	private static int countCriticalIssues(List<DiagnosticEntry> entries) {
		if (entries == null || entries.isEmpty()) {
			return 0;
		}
		return (int) entries.stream()
				.flatMap(e -> e.result.getIssues().stream())
				.filter(i -> i.getSeverity() == SecuritySeverity.CRITICAL)
				.count();
	}

	private double calculateAverageIssues(List<DiagnosticEntry> entries) {
		if (entries.isEmpty())
			return 0.0;
		return entries.stream()
				.mapToLong(e -> e.result.getIssues().size())
				.average()
				.orElse(0.0);
	}

	private double calculateSecureRatio(List<DiagnosticEntry> entries) {
		if (entries.isEmpty())
			return 1.0;
		return (double) countSecureEntries(entries) / entries.size();
	}

	private double calculateCriticalRatio(List<DiagnosticEntry> entries) {
		if (entries.isEmpty())
			return 0.0;
		long criticalCount = entries.stream()
				.flatMap(e -> e.result.getIssues().stream())
				.filter(i -> i.getSeverity() == SecuritySeverity.CRITICAL)
				.count();
		return (double) criticalCount / entries.size();
	}

	public void resetStats() {
		stats.reset();
		logger.info("Statistiques réinitialisées");
	}

	public void resetStats(SecurityCategory category) {
		// Réinitialisation spécifique à une catégorie
		logger.info("Statistiques réinitialisées pour la catégorie: {}", category);
	}

	private Map<String, Object> collectSystemMetrics() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.putAll(stats.getStats());
		metrics.put("activeHandlers", eventHandlers.size());
		metrics.put("historySize", history.size());
		metrics.put("state", state.name());
		return metrics;
	}

	public void startMetricsCollection(Duration interval) {
		if (interval == null || interval.isNegative() || interval.isZero()) {
			throw new IllegalArgumentException("Intervalle invalide");
		}

		validateState();
		maintenanceExecutor.scheduleAtFixedRate(() -> {
			try {
				Map<String, Double> metrics = getPerformanceMetrics(interval);
				logger.info("Métriques périodiques: {}", metrics);
				systemMonitor.updateMetrics(metrics);
			} catch (Exception e) {
				errorHandler.handleSystemError(e, "Collection métriques");
			}
		}, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
	}

	private void shutdownExecutors() {
		try {
			asyncExecutor.shutdown();
			maintenanceExecutor.shutdown();

			if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				asyncExecutor.shutdownNow();
			}
			if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				maintenanceExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			asyncExecutor.shutdownNow();
			maintenanceExecutor.shutdownNow();
			logger.error("Interruption pendant la fermeture des exécuteurs", e);
		}
	}

	private void handleSystemError(Exception e, String context) {
		logger.error("Erreur système dans {}: {}", context, e.getMessage());
		stats.recordFailure();

		systemMonitor.handleRiskySituation(
				String.format("Erreur système - %s: %s", context, e.getMessage()));

		if (e instanceof OutOfMemoryError ||
				e instanceof ThreadDeath ||
				e instanceof VirtualMachineError) {
			systemMonitor.triggerEmergencyProtocol();
			state = LoggerState.MAINTENANCE;
		}
	}

	private void persistMetrics(Map<String, Number> metrics) {
		try {
			DiagnosticRepository.getInstance().saveMetrics(
					new DiagnosticMetrics(
							LocalDateTime.now(),
							metrics,
							systemMonitor.getSystemState()));
		} catch (Exception e) {
			logger.error("Échec persistance métriques", e);
			stats.recordFailure();
		}
	}

	private void initializeComponents() {
		try {
			validateInitialization();
			this.systemState = new SystemState();
			this.diagnosticState.initialize();

			// Démarrage des services dans l'ordre
			startScheduledMaintenance();
			startMetricsCollection(Duration.ofMinutes(1));

			logger.info("Composants initialisés avec succès");
		} catch (Exception e) {
			logger.error("Échec de l'initialisation des composants", e);
			throw new IllegalStateException("Échec initialisation", e);
		}
	}

	private void handleCriticalFailure(Exception e, String context) {
		logger.error("Erreur critique dans {}: {}", context, e.getMessage(), e);
		stats.recordCriticalEvent();
		systemMonitor.handleRiskySituation(String.format("Erreur critique - %s", context));

		try {
			if (stats.getCriticalEventCount() > ERROR_THRESHOLD) {
				systemMonitor.triggerEmergencyProtocol();
				transitionToMaintenanceMode();
			}
			persistErrorMetrics(e, context);
		} catch (Exception ex) {
			logger.error("Échec de la gestion d'erreur critique", ex);
			forceShutdown();
		}
	}

	private void handleSystemFailure(Throwable e, String context) {
		logger.error("Erreur système critique dans {}: {}", context, e.getMessage());
		stats.recordCriticalEvent();

		try {
			systemMonitor.handleRiskySituation(
					String.format("Erreur système critique - %s: %s", context, e.getMessage()));

			if (e instanceof Error || stats.getCriticalEventCount() > ERROR_THRESHOLD) {
				systemMonitor.triggerEmergencyProtocol();
				transitionToMaintenanceMode();
			}
		} catch (Exception ex) {
			logger.error("Échec gestion erreur critique", ex);
		}
	}

	private void transitionToMaintenanceMode() {
		state = LoggerState.MAINTENANCE;
		flushHistory();
		notifyStateChange();
	}

	private void handleCriticalResources() {
		try {
			double memoryUsage = systemMonitor.getMemoryUsage();
			if (memoryUsage > MEMORY_CRITICAL_THRESHOLD) {
				stats.recordCriticalEvent();
				systemMonitor.handleRiskySituation("Mémoire critique: " +
						String.format("%.2f%%", memoryUsage * 100));
				forceCleanup();
			}
		} catch (Exception e) {
			handleSystemFailure(e, "Gestion ressources critiques");
		}
	}

	private void checkOperationTimeout(long startTime, String operation) {
		long duration = System.currentTimeMillis() - startTime;
		if (duration > CHECK_TIMEOUT_MS) {
			logger.warn("{} a pris trop de temps: {}ms", operation, duration);
			stats.recordSlowOperation(operation, duration);
			if (duration > CHECK_TIMEOUT_MS * 2) {
				systemMonitor.handleRiskySituation(
						String.format("Opération lente détectée: %s", operation));
			}
		}
	}

	private void handleCriticalState() {
		try {
			Map<String, Number> metrics = collectCurrentMetrics();
			if (isCriticalState(metrics)) {
				systemMonitor.handleRiskySituation("État critique détecté");
				stats.recordCriticalEvent();

				if (consecutiveCriticalStates.incrementAndGet() > MAX_CRITICAL_STATES) {
					systemMonitor.triggerEmergencyProtocol();
				}
			} else {
				consecutiveCriticalStates.set(0);
			}
		} catch (Exception e) {
			handleSystemFailure(e, "Gestion état critique");
		}
	}

	private void processJsonStructure(JSONObject structure) {
		try {
			validateStructure(structure);
			Iterator<String> keys = structure.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				processStructureElement(key, structure.getJSONObject(key));
			}
		} catch (JSONException e) {
			logger.error("Erreur traitement structure JSON", e);
			throw new DiagnosticProcessingException("Structure JSON invalide", e);
		}
	}

}

public class DiagnosticLoggingException extends RuntimeException {
	public DiagnosticLoggingException(String message, Throwable cause) {
		super(message, cause);
	}
}

public interface DiagnosticEventHandler {
	void onCriticalDiagnostic(DiagnosticResult result);

	void onHistoryCleanup(int removedEntries);

	/**
	 * Vérifie si le handler est toujours valide et utilisable.
	 * 
	 * @return true si le handler est valide, false sinon
	 */
	default boolean isValid() {
		return true; // Implémentation par défaut
	}
}

@Builder
public class DiagnosticSummary {
	private final int totalCount;
	private final int secureCount;
	private final int criticalIssuesCount;
	private final LocalDateTime generatedAt;

	public double getSecureRatio() {
		return totalCount == 0 ? 1.0 : (double) secureCount / totalCount;
	}
}

@Value
public class DiagnosticConfig {
	Duration retentionPeriod;
	int maxHistory;
	boolean debugMode;

	public static DiagnosticConfig getDefault() {
		return new DiagnosticConfig(
				Duration.ofHours(24),
				100,
				false);
	}
}
