package org.orgaprop.test7.security.diagnostic.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Gestionnaire spécialisé pour les erreurs de diagnostic.
 * Gère les erreurs spécifiques au contexte des diagnostics de sécurité.
 */
public class DiagnosticErrorHandler {
	private static final Logger logger = LoggerFactory.getLogger(DiagnosticErrorHandler.class);
	private final UnifiedSystemMonitoring systemMonitor;
	private final PerformanceStats stats;

	public DiagnosticErrorHandler(UnifiedSystemMonitoring systemMonitor, PerformanceStats stats) {
		this.systemMonitor = systemMonitor;
		this.stats = stats;
	}

	// Méthodes à extraire de DiagnosticLogger :

	/**
	 * Gère une erreur système générique.
	 */
	public void handleSystemError(Exception e, String context) {
		logger.error("Erreur système dans {}: {}", context, e.getMessage());
		stats.recordFailure();

		systemMonitor.handleRiskySituation(
				String.format("Erreur système - %s: %s", context, e.getMessage()));

		if (e instanceof OutOfMemoryError ||
				e instanceof ThreadDeath ||
				e instanceof VirtualMachineError) {
			systemMonitor.triggerEmergencyProtocol();
			// Note: state management should be handled by the caller
		}
	}

	/**
	 * Gère une erreur critique du système.
	 */
	public void handleCriticalFailure(Exception e, String context) {
		logger.error("Erreur critique dans {}: {}", context, e.getMessage(), e);
		stats.recordCriticalEvent();
		systemMonitor.handleRiskySituation(String.format("Erreur critique - %s", context));

		try {
			if (stats.getCriticalEventCount() > ERROR_THRESHOLD) {
				systemMonitor.triggerEmergencyProtocol();
			}
			// Note: state management should be handled by the caller
		} catch (Exception ex) {
			logger.error("Échec de la gestion d'erreur critique", ex);
		}
	}

	/**
	 * Gère un événement critique du diagnostic.
	 */
	public void handleCriticalEvent(DiagnosticEntry entry, List<SecurityIssue> criticalIssues) {
		String message = String.format("Issues critiques (%d) - %s",
				criticalIssues.size(), entry.context);

		try {
			logger.error(message);
			criticalIssues.forEach(issue -> logger.error("Issue critique: {} - {}",
					issue.getSeverity(), issue.getMessage()));

			systemMonitor.handleRiskySituation(message);
			stats.recordCriticalEvent();

			if (criticalIssues.size() > ERROR_THRESHOLD) {
				systemMonitor.triggerEmergencyProtocol();
			}
		} catch (Exception e) {
			handleSystemError(e, "Traitement événement critique");
		}
	}

	/**
	 * Gère une erreur de maintenance.
	 */
	public void handleMaintenanceFailure(Exception e) {
		logger.error("Échec critique de la maintenance", e);
		systemMonitor.handleRiskySituation("Échec maintenance DiagnosticLogger");
		stats.recordFailure();
	}
}
