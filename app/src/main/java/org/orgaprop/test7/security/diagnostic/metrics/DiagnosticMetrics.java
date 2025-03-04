package org.orgaprop.test7.security.diagnostic.metrics;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.EnumMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.orgaprop.test7.security.diagnostic.DiagnosticResult;
import org.orgaprop.test7.security.diagnostic.SecurityCategory;

/**
 * Gère les métriques de diagnostic de sécurité.
 * Cette classe est thread-safe grâce à l'utilisation de structures
 * concurrentes.
 */
public class DiagnosticMetrics {
	private static final Logger logger = LoggerFactory.getLogger(DiagnosticMetrics.class);

	/** Compteurs d'issues par catégorie */
	private final EnumMap<SecurityCategory, AtomicInteger> issueCountsByCategory = new EnumMap<>(SecurityCategory.class);

	/** Nombre total de diagnostics effectués */
	private final AtomicInteger totalDiagnostics = new AtomicInteger();

	/** Timestamp du dernier diagnostic */
	@Getter
	private volatile LocalDateTime lastDiagnosticTime;

	/**
	 * Enregistre le résultat d'un diagnostic.
	 *
	 * @param result Résultat du diagnostic à enregistrer
	 * @throws NullPointerException si result est null
	 */
	public void recordDiagnosticResult(DiagnosticResult result) {
		if (result == null) {
			throw new NullPointerException("Le résultat ne peut pas être null");
		}

		totalDiagnostics.incrementAndGet();
		lastDiagnosticTime = LocalDateTime.now();

		result.getIssues().forEach(issue -> issueCountsByCategory
				.computeIfAbsent(issue.getCategory(), k -> new AtomicInteger())
				.incrementAndGet());

		logger.debug("Diagnostic enregistré - Total: {}, Issues: {}",
				totalDiagnostics.get(), result.getIssues().size());
	}

	/**
	 * Retourne les statistiques actuelles.
	 *
	 * @return Map non modifiable des statistiques
	 */
	public Map<String, Number> getStats() {
		Map<String, Number> stats = new HashMap<>();
		stats.put("totalDiagnostics", totalDiagnostics.get());
		issueCountsByCategory.forEach((category, count) -> stats.put("issues." + category.name(), count.get()));
		return Collections.unmodifiableMap(stats);
	}

	/**
	 * Réinitialise toutes les métriques.
	 */
	public void reset() {
		issueCountsByCategory.clear();
		totalDiagnostics.set(0);
		lastDiagnosticTime = null;
		logger.info("Métriques réinitialisées");
	}
}
