package org.orgaprop.test7.metrics.quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class QualityManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(QualityManager.class);

	private final BlockingQueue<QualityCheck> checkQueue;
	private final ExecutorService executor;
	private final AtomicBoolean isRunning;
	private final QualityMetrics metrics;
	private final MetricsQuality qualityService;
	private final AlertManager alertManager;
	private final MetricsConfig config;
	private final ConfigModule qualityConfig;

	public QualityManager(AlertManager alertManager) {
		this.config = MetricsConfig.getInstance();
		this.qualityConfig = config.getModule("quality");

		if (qualityConfig == null) {
			logger.error("Configuration 'quality' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> processingConfig = (Map<String, Object>) qualityConfig.getProperty("processing");
		int maxQueueSize = (int) processingConfig.get("maxQueueSize");
		int threadPoolSize = (int) processingConfig.get("threadPoolSize");

		this.checkQueue = new LinkedBlockingQueue<>(maxQueueSize);
		this.executor = Executors.newFixedThreadPool(threadPoolSize);
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new QualityMetrics();
		this.qualityService = new MetricsQuality(qualityConfig);
		this.alertManager = alertManager;

		startQualityChecking();
	}

	public void submitCheck(String metricType, Object value) {
		if (!isRunning.get()) {
			logger.warn("Tentative de vérification alors que le QualityManager est arrêté");
			return;
		}

		QualityCheck check = new QualityCheck(metricType, value);
		if (!checkQueue.offer(check)) {
			handleQueueFull(check);
			return;
		}
		metrics.recordSubmission(metricType);
	}

	private void startQualityChecking() {
		int processors = Runtime.getRuntime().availableProcessors();
		for (int i = 0; i < processors; i++) {
			executor.submit(this::processQualityChecks);
		}
	}

	private void processQualityChecks() {
		while (isRunning.get()) {
			try {
				QualityCheck check = checkQueue.poll(1, TimeUnit.SECONDS);
				if (check != null) {
					processCheck(check);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void processCheck(QualityCheck check) {
		long startTime = System.nanoTime();
		try {
			MetricsQuality.ValidationResult result = qualityService.validateMetric(check.metricType, check.value);

			if (!result.isValid()) {
				handleQualityIssue(check, result);
			}

			if (check.value instanceof Number) {
				checkForAnomaly(check.metricType, ((Number) check.value).doubleValue());
			}

			metrics.recordCheck(check.metricType, System.nanoTime() - startTime);
		} catch (Exception e) {
			handleCheckError(e, check);
		}
	}

	private void checkForAnomaly(String metricType, double value) {
		if (qualityService.detectAnomaly(new MetricsQuality.MetricData(metricType, value))) {
			handleAnomaly(metricType, value);
			metrics.recordAnomaly(metricType);
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				executor.shutdownNow();
			}
		}
	}

	private static class QualityCheck {
		final String metricType;
		final Object value;
		final long timestamp;

		QualityCheck(String metricType, Object value) {
			this.metricType = metricType;
			this.value = value;
			this.timestamp = System.currentTimeMillis();
		}
	}

	private static class QualityMetrics {
		private final Map<String, AtomicInteger> checksByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> anomaliesByType = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> processingTimeByType = new ConcurrentHashMap<>();
		private final AtomicLong totalChecks = new AtomicLong(0);

		void recordSubmission(String metricType) {
			incrementCounter(checksByType, metricType);
			totalChecks.incrementAndGet();
		}

		void recordCheck(String metricType, long duration) {
			processingTimeByType.computeIfAbsent(metricType, k -> new AtomicLong())
					.addAndGet(duration);
		}

		void recordAnomaly(String metricType) {
			incrementCounter(anomaliesByType, metricType);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalChecks", totalChecks.get(),
					"checksByType", new HashMap<>(checksByType),
					"anomalies", new HashMap<>(anomaliesByType),
					"averageProcessingTimes", calculateAverageProcessingTimes());
		}

		private Map<String, Double> calculateAverageProcessingTimes() {
			Map<String, Double> averages = new HashMap<>();
			checksByType.forEach((type, checks) -> {
				long totalTime = processingTimeByType.getOrDefault(type, new AtomicLong()).get();
				averages.put(type, checks.get() > 0 ? (double) totalTime / checks.get() : 0);
			});
			return averages;
		}
	}
}
