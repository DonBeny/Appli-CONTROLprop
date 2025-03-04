package org.orgaprop.test7.metrics.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import android.os.Process;
import android.os.SystemClock;
import android.system.Os;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class SystemMetricsIntegrator implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(SystemMetricsIntegrator.class);

	private final ScheduledExecutorService scheduler;
	private final ConcurrentNavigableMap<Long, SystemMetrics> metricsHistory;
	private final AtomicBoolean isRunning;
	private final IntegrationMetrics metrics;
	private final AlertManager alertManager;
	private final Runtime runtime;
	private final Map<MetricType, MetricCollector> collectors;
	private final SystemResourceMonitor resourceMonitor;
	private final MetricsConfig config;
	private final ConfigModule integrationConfig;

	public SystemMetricsIntegrator(AlertManager alertManager) {
		this.config = MetricsConfig.getInstance();
		this.integrationConfig = config.getModule("system_integration");

		if (integrationConfig == null) {
			logger.error("Configuration 'system_integration' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> collectionConfig = (Map<String, Object>) integrationConfig.getProperty("collection");
		int historySize = (int) collectionConfig.get("historySize");

		this.scheduler = createScheduler();
		this.metricsHistory = new ConcurrentSkipListMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new IntegrationMetrics();
		this.alertManager = alertManager;
		this.runtime = Runtime.getRuntime();
		this.collectors = initializeCollectors();
		this.resourceMonitor = new SystemResourceMonitor();

		startCollection();
	}

	private Map<MetricType, MetricCollector> initializeCollectors() {
		Map<String, Object> collectorsConfig = (Map<String, Object>) integrationConfig.getProperty("collectors");
		List<String> types = (List<String>) collectorsConfig.get("types");

		Map<MetricType, MetricCollector> collectors = new ConcurrentHashMap<>();
		for (String type : types) {
			MetricType metricType = MetricType.valueOf(type);
			collectors.put(metricType, createCollector(metricType));
		}
		return collectors;
	}

	private void startCollection() {
		Map<String, Object> collectionConfig = (Map<String, Object>) integrationConfig.getProperty("collection");
		long interval = (long) collectionConfig.get("interval");

		scheduler.scheduleAtFixedRate(
				this::collectMetrics,
				0,
				interval,
				TimeUnit.MILLISECONDS);
	}

	private void collectMetrics() {
		try {
			SystemMetrics currentMetrics = gatherSystemMetrics();
			updateHistory(currentMetrics);
			analyzeMetrics(currentMetrics);
			metrics.recordCollection();
		} catch (Exception e) {
			handleCollectionError(e);
		}
	}

	private SystemMetrics gatherSystemMetrics() {
		Map<MetricType, Object> collectedData = new ConcurrentHashMap<>();
		collectors.forEach((type, collector) -> {
			try {
				collectedData.put(type, collector.collect());
			} catch (Exception e) {
				handleCollectorError(type, e);
			}
		});

		return new SystemMetrics(collectedData, System.currentTimeMillis());
	}

	private void analyzeMetrics(SystemMetrics currentMetrics) {
		Map<String, Object> thresholdsConfig = (Map<String, Object>) integrationConfig.getProperty("thresholds");

		// Vérification mémoire
		Map<String, Object> memoryThresholds = (Map<String, Object>) thresholdsConfig.get("memory");
		if (currentMetrics.getMemoryUsage() > (double) memoryThresholds.get("warningLevel")) {
			alertManager.raiseAlert("HIGH_MEMORY_USAGE", currentMetrics.getMemoryMetrics());
		}

		// Vérification CPU
		Map<String, Object> cpuThresholds = (Map<String, Object>) thresholdsConfig.get("cpu");
		if (currentMetrics.getCpuUsage() > (double) cpuThresholds.get("warningLevel")) {
			alertManager.raiseAlert("HIGH_CPU_USAGE", currentMetrics.getCpuMetrics());
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
			resourceMonitor.close();
		}
	}

	private static class SystemMetrics {
		private final Map<MetricType, Object> metrics;
		private final long timestamp;

		SystemMetrics(Map<MetricType, Object> metrics, long timestamp) {
			this.metrics = new HashMap<>(metrics);
			this.timestamp = timestamp;
		}

		boolean isMemoryThresholdExceeded() {
			MemoryMetrics memMetrics = (MemoryMetrics) metrics.get(MetricType.MEMORY);
			return memMetrics != null && memMetrics.getUsageRatio() > 0.85;
		}

		boolean isCpuThresholdExceeded() {
			CpuMetrics cpuMetrics = (CpuMetrics) metrics.get(MetricType.CPU);
			return cpuMetrics != null && cpuMetrics.getUsage() > 80.0;
		}

		Map<String, Object> getMemoryMetrics() {
			return ((MemoryMetrics) metrics.get(MetricType.MEMORY)).toMap();
		}

		Map<String, Object> getCpuMetrics() {
			return ((CpuMetrics) metrics.get(MetricType.CPU)).toMap();
		}
	}

	private enum MetricType {
		MEMORY, CPU, THREAD, IO
	}

	private static class IntegrationMetrics {
		private final AtomicLong collectionCount = new AtomicLong(0);
		private final AtomicLong errorCount = new AtomicLong(0);
		private final Map<MetricType, AtomicLong> collectorErrors = new ConcurrentHashMap<>();

		void recordCollection() {
			collectionCount.incrementAndGet();
		}

		void recordError(MetricType type) {
			errorCount.incrementAndGet();
			collectorErrors.computeIfAbsent(type, k -> new AtomicLong())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"collections", collectionCount.get(),
					"errors", errorCount.get(),
					"collectorErrors", new HashMap<>(collectorErrors));
		}
	}
}
