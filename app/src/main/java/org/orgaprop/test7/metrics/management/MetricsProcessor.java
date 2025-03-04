package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MetricsProcessor implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsProcessor.class);

	private static final int BATCH_SIZE = 100;
	private static final long PROCESSING_INTERVAL = 1000; // 1 seconde
	private static final int MAX_QUEUE_SIZE = 10_000;

	private final BlockingQueue<MetricEntry> processingQueue;
	private final ScheduledExecutorService scheduler;
	private final StatisticsManager statisticsManager;
	private final AlertManager alertManager;
	private final ProcessingMetrics metrics;
	private final AtomicBoolean isRunning;

	public MetricsProcessor(AlertManager alertManager) {
		this.processingQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
		this.scheduler = createScheduler();
		this.statisticsManager = new StatisticsManager(24 * 60 * 60 * 1000); // 24h rétention
		this.alertManager = alertManager;
		this.metrics = new ProcessingMetrics();
		this.isRunning = new AtomicBoolean(true);

		startProcessing();
	}

	public void submitMetric(String name, double value, Map<String, Object> metadata) {
		if (!isRunning.get()) {
			logger.warn("Tentative de soumission alors que le processor est arrêté");
			return;
		}

		try {
			MetricEntry entry = new MetricEntry(name, value, metadata);
			if (!processingQueue.offer(entry)) {
				handleQueueFull(entry);
				return;
			}
			metrics.recordSubmission();
		} catch (Exception e) {
			handleSubmissionError(e, name);
		}
	}

	private void startProcessing() {
		scheduler.scheduleAtFixedRate(
				this::processBatch,
				PROCESSING_INTERVAL,
				PROCESSING_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void processBatch() {
		List<MetricEntry> batch = new ArrayList<>(BATCH_SIZE);
		processingQueue.drainTo(batch, BATCH_SIZE);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processMetricBatch(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	private void processMetricBatch(List<MetricEntry> batch) {
		try {
			for (MetricEntry entry : batch) {
				processMetric(entry);
			}
			updateStatistics(batch);
		} catch (Exception e) {
			handleProcessingError(e, batch.size());
		}
	}

	private void processMetric(MetricEntry entry) {
		validateMetric(entry);
		enrichMetric(entry);
		statisticsManager.recordValue(entry.name, entry.value, System.currentTimeMillis());
		checkThresholds(entry);
	}

	private void validateMetric(MetricEntry entry) {
		if (entry.name == null || entry.name.trim().isEmpty()) {
			throw new IllegalArgumentException("Nom de métrique invalide");
		}
		if (Double.isNaN(entry.value) || Double.isInfinite(entry.value)) {
			throw new IllegalArgumentException("Valeur de métrique invalide");
		}
	}

	private void enrichMetric(MetricEntry entry) {
		entry.metadata.put("timestamp", System.currentTimeMillis());
		entry.metadata.put("processingTime", System.nanoTime());
	}

	private void checkThresholds(MetricEntry entry) {
		// Implémentation de la vérification des seuils
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
			cleanup();
		}
	}

	private static class MetricEntry {
		final String name;
		final double value;
		final Map<String, Object> metadata;
		final long timestamp;

		MetricEntry(String name, double value, Map<String, Object> metadata) {
			this.name = name;
			this.value = value;
			this.metadata = new HashMap<>(metadata);
			this.timestamp = System.currentTimeMillis();
		}
	}

	private static class ProcessingMetrics {
		private final AtomicLong submittedMetrics = new AtomicLong(0);
		private final AtomicLong processedMetrics = new AtomicLong(0);
		private final AtomicLong processingErrors = new AtomicLong(0);
		private final AtomicLong totalProcessingTime = new AtomicLong(0);
		private final AtomicLong batchesProcessed = new AtomicLong(0);

		void recordSubmission() {
			submittedMetrics.incrementAndGet();
		}

		void recordBatchProcessing(int size, long duration) {
			processedMetrics.addAndGet(size);
			totalProcessingTime.addAndGet(duration);
			batchesProcessed.incrementAndGet();
		}

		void recordError() {
			processingErrors.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"submitted", submittedMetrics.get(),
					"processed", processedMetrics.get(),
					"errors", processingErrors.get(),
					"batches", batchesProcessed.get(),
					"averageProcessingTime", getAverageProcessingTime());
		}

		private double getAverageProcessingTime() {
			long batches = batchesProcessed.get();
			return batches > 0 ? (double) totalProcessingTime.get() / batches : 0;
		}
	}
}
