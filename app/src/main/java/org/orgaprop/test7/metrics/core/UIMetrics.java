package org.orgaprop.test7.metrics.core;

import android.content.Context;
import android.view.View;
import androidx.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.io.File;
import java.io.IOException;

public class UIMetrics {
	private static final Logger logger = LoggerFactory.getLogger(UIMetrics.class);

	private final Map<Integer, AtomicInteger> layoutUsageCount;
	private final Map<String, AtomicInteger> interactionCount;
	private final AtomicLong totalInteractionTime;
	private final AtomicInteger layoutChangeCount;
	private volatile long lastInteractionTime;
	private volatile long lastLayoutChangeTime;
	private volatile int currentLayout;

	private final MetricsPersistenceManager persistenceManager;
	private final MetricsBackupManager backupManager;
	private final String sessionId;
	private final MetricsConfig config;
	private final ConfigModule uiConfig;

	public UIMetrics(MetricsPersistenceManager persistenceManager,
			MetricsBackupManager backupManager) {
		this.config = MetricsConfig.getInstance();
		this.uiConfig = config.getModule("ui_metrics");

		if (uiConfig == null) {
			logger.error("Configuration 'ui_metrics' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> aggregationConfig = (Map<String, Object>) uiConfig.getProperty("aggregation");
		boolean trackLayout = (boolean) aggregationConfig.get("layoutUsageEnabled");
		boolean trackInteractions = (boolean) aggregationConfig.get("interactionTrackingEnabled");

		this.layoutUsageCount = trackLayout ? new ConcurrentHashMap<>() : null;
		this.interactionCount = trackInteractions ? new ConcurrentHashMap<>() : null;
		this.totalInteractionTime = new AtomicLong(0);
		this.layoutChangeCount = new AtomicInteger(0);
		this.lastInteractionTime = System.currentTimeMillis();
		this.lastLayoutChangeTime = System.currentTimeMillis();
		this.currentLayout = -1;

		this.persistenceManager = persistenceManager;
		this.backupManager = backupManager;
		this.sessionId = generateSessionId();

		Map<String, Object> sessionConfig = (Map<String, Object>) uiConfig.getProperty("session");
		if ((boolean) sessionConfig.get("autoRestore")) {
			restoreState();
		}
	}

	private String generateSessionId() {
		return "session_" + System.currentTimeMillis();
	}

	private void restoreState() {
		try {
			Map<String, Object> state = persistenceManager.loadState(sessionId);
			if (state != null) {
				applyRestoredState(state);
			}
		} catch (Exception e) {
			logger.error("Erreur lors de la restauration de l'état", e);
		}
	}

	private void applyRestoredState(Map<String, Object> metrics) {
		try {
			// Restaurer les compteurs
			@SuppressWarnings("unchecked")
			Map<Integer, Integer> layoutUsage = (Map<Integer, Integer>) metrics.get("layoutUsageCount");
			if (layoutUsage != null) {
				layoutUsage.forEach((layout, count) -> layoutUsageCount.put(layout, new AtomicInteger(count)));
			}

			@SuppressWarnings("unchecked")
			Map<String, Integer> interactions = (Map<String, Integer>) metrics.get("interactionCount");
			if (interactions != null) {
				interactions.forEach((type, count) -> interactionCount.put(type, new AtomicInteger(count)));
			}

			Long totalTime = (Long) metrics.get("totalInteractionTime");
			if (totalTime != null) {
				totalInteractionTime.set(totalTime);
			}

			// Restaurer les timestamps
			lastInteractionTime = (Long) metrics.getOrDefault("lastInteractionTime",
					System.currentTimeMillis());
			lastLayoutChangeTime = (Long) metrics.getOrDefault("lastLayoutChangeTime",
					System.currentTimeMillis());
		} catch (Exception e) {
			logger.error("Erreur lors de l'application des métriques restaurées", e);
		}
	}

	@Override
	public void recordLayoutChange(@NonNull Integer newLayout) {
		layoutUsageCount.computeIfAbsent(newLayout, k -> new AtomicInteger(0))
				.incrementAndGet();
		layoutChangeCount.incrementAndGet();

		long now = System.currentTimeMillis();
		if (currentLayout != -1) {
			recordLayoutDuration(currentLayout, now - lastLayoutChangeTime);
		}

		lastLayoutChangeTime = now;
		currentLayout = newLayout;
		logger.debug("Layout changé: {} (Total changements: {})",
				newLayout, layoutChangeCount.get());
	}

	@Override
	public void recordInteraction(String interactionType) {
		interactionCount.computeIfAbsent(interactionType, k -> new AtomicInteger(0))
				.incrementAndGet();
		lastInteractionTime = System.currentTimeMillis();
		logger.debug("Interaction enregistrée: {}", interactionType);
	}

	public void recordInteractionDuration(long durationMs) {
		totalInteractionTime.addAndGet(durationMs);
	}

	private void recordLayoutDuration(int layout, long durationMs) {
		// Enregistrement optionnel de la durée passée sur chaque layout
	}

	@Override
	public Map<String, Object> getMetrics() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("layoutUsageCount", getLayoutUsageSnapshot());
		metrics.put("interactionCount", getInteractionSnapshot());
		metrics.put("totalInteractionTime", totalInteractionTime.get());
		metrics.put("layoutChangeCount", layoutChangeCount.get());
		metrics.put("lastInteractionTime", lastInteractionTime);
		metrics.put("lastLayoutChangeTime", lastLayoutChangeTime);
		metrics.put("currentLayout", currentLayout);
		return metrics;
	}

	private Map<Integer, Integer> getLayoutUsageSnapshot() {
		Map<Integer, Integer> snapshot = new HashMap<>();
		layoutUsageCount.forEach((layout, count) -> snapshot.put(layout, count.get()));
		return snapshot;
	}

	private Map<String, Integer> getInteractionSnapshot() {
		Map<String, Integer> snapshot = new HashMap<>();
		interactionCount.forEach((type, count) -> snapshot.put(type, count.get()));
		return snapshot;
	}

	@Override
	public void reset() {
		try {
			// Sauvegarder l'état final avant reset
			persistenceManager.saveMetrics(sessionId + "_final", getMetrics());
			backupManager.backupMetrics(getMetrics());

			// Réinitialiser tous les compteurs
			layoutUsageCount.clear();
			interactionCount.clear();
			totalInteractionTime.set(0);
			layoutChangeCount.set(0);
			lastInteractionTime = System.currentTimeMillis();
			lastLayoutChangeTime = System.currentTimeMillis();
			currentLayout = -1;

			logger.info("Métriques UI réinitialisées");
		} catch (Exception e) {
			logger.error("Erreur lors de la réinitialisation des métriques", e);
		}
	}

	public void cleanup() {
		Map<String, Object> cleanupConfig = (Map<String, Object>) uiConfig.getProperty("cleanup");
		if ((boolean) cleanupConfig.get("enabled")) {
			long oldestAllowed = (long) cleanupConfig.get("oldestAllowedData");
			persistenceManager.cleanup(oldestAllowed);
		}
	}

	/**
	 * Filtre les métriques selon des critères spécifiques
	 */
	public List<Map<String, Object>> getFilteredMetrics(MetricsFilter.FilterCriteria criteria) {
		List<Map<String, Object>> allMetrics = getMetricsHistory();
		return metricsFilter.filterMetrics(allMetrics, criteria);
	}

	/**
	 * Obtient des métriques agrégées selon une période
	 */
	public Map<String, Object> getAggregatedMetrics(long startTime, long endTime) {
		MetricsFilter.FilterCriteria criteria = new MetricsFilter.FilterCriteria.Builder()
				.setTimeRange(startTime, endTime)
				.build();

		List<Map<String, Object>> filtered = getFilteredMetrics(criteria);
		return metricsFilter.aggregateMetrics(filtered);
	}

	/**
	 * Obtient des métriques pour un type d'interaction spécifique
	 */
	public List<Map<String, Object>> getInteractionMetrics(String interactionType) {
		MetricsFilter.FilterCriteria criteria = new MetricsFilter.FilterCriteria.Builder()
				.setInteractionTypes(Set.of(interactionType))
				.build();

		return getFilteredMetrics(criteria);
	}

	/**
	 * Obtient des métriques pour un layout spécifique
	 */
	public List<Map<String, Object>> getLayoutMetrics(int layoutType) {
		MetricsFilter.FilterCriteria criteria = new MetricsFilter.FilterCriteria.Builder()
				.setLayoutTypes(Set.of(layoutType))
				.build();

		return getFilteredMetrics(criteria);
	}

	/**
	 * Obtient les métriques de performance
	 */
	public Map<String, Object> getPerformanceMetrics(long maxResponseTime) {
		MetricsFilter.FilterCriteria criteria = new MetricsFilter.FilterCriteria.Builder()
				.setMaxResponseTime(maxResponseTime)
				.build();

		List<Map<String, Object>> filtered = getFilteredMetrics(criteria);
		return metricsFilter.aggregateMetrics(filtered);
	}

	/**
	 * Exporte les métriques dans le format spécifié
	 */
	public File exportMetrics(MetricsExporter.ExportFormat format) throws IOException {
		List<Map<String, Object>> allMetrics = getCompleteMetricsHistory();
		return metricsExporter.exportMetrics(allMetrics, format);
	}

	/**
	 * Récupère l'historique complet des métriques avec agrégation
	 */
	private List<Map<String, Object>> getCompleteMetricsHistory() {
		List<Map<String, Object>> history = getMetricsHistory();

		// Ajouter les métriques agrégées
		Map<String, Object> aggregatedMetrics = new HashMap<>();
		aggregatedMetrics.put("totalLayouts", layoutUsageCount.size());
		aggregatedMetrics.put("totalInteractions", interactionCount.size());
		aggregatedMetrics.put("averageInteractionTime", calculateAverageInteractionTime());
		aggregatedMetrics.put("totalLayoutChanges", layoutChangeCount.get());

		history.add(aggregatedMetrics);
		return history;
	}

	/**
	 * Exporte les métriques d'une période spécifique
	 */
	public File exportMetricsForPeriod(long startTime, long endTime,
			MetricsExporter.ExportFormat format) throws IOException {
		MetricsFilter.FilterCriteria criteria = new MetricsFilter.FilterCriteria.Builder()
				.setTimeRange(startTime, endTime)
				.build();

		List<Map<String, Object>> filteredMetrics = getFilteredMetrics(criteria);
		return metricsExporter.exportMetrics(filteredMetrics, format);
	}

	/**
	 * Génère un rapport personnalisé des métriques
	 */
	public File generateCustomReport(MetricsFilter.FilterCriteria criteria,
			MetricsExporter.ExportFormat format) throws IOException {
		List<Map<String, Object>> filteredMetrics = getFilteredMetrics(criteria);
		Map<String, Object> aggregatedData = metricsFilter.aggregateMetrics(filteredMetrics);

		List<Map<String, Object>> reportData = new ArrayList<>();
		reportData.add(aggregatedData);
		reportData.addAll(filteredMetrics);

		return metricsExporter.exportMetrics(reportData, format);
	}

	/**
	 * Planifie l'export automatique des métriques
	 */
	public void scheduleMetricsExport(MetricsExporter.ExportFormat format,
			long intervalMillis) {
		// Implémentation de l'export périodique
		// Utiliser WorkManager ou AlarmManager selon les besoins
	}

	/**
	 * Obtient la vue de visualisation des métriques
	 */
	public View getVisualizationView() {
		return metricsVisualizer.getChartView();
	}

	/**
	 * Met à jour la visualisation avec de nouvelles données
	 */
	public void updateVisualization(MetricsVisualizer.ChartType chartType) {
		Map<String, Object> currentMetrics = getMetrics();
		metricsVisualizer.updateData(currentMetrics, chartType);
	}

	/**
	 * Crée une visualisation des métriques filtrées
	 */
	public View getFilteredVisualization(MetricsFilter.FilterCriteria criteria,
			MetricsVisualizer.ChartType chartType) {
		List<Map<String, Object>> filtered = getFilteredMetrics(criteria);
		Map<String, Object> aggregated = metricsFilter.aggregateMetrics(filtered);
		metricsVisualizer.updateData(aggregated, chartType);
		return metricsVisualizer.getChartView();
	}

	/**
	 * Crée une visualisation des métriques de performance
	 */
	public View getPerformanceVisualization() {
		Map<String, Object> performanceData = performanceMetrics.getPerformanceReport();
		metricsVisualizer.updateData(performanceData, MetricsVisualizer.ChartType.LINE_CHART);
		return metricsVisualizer.getChartView();
	}
}
