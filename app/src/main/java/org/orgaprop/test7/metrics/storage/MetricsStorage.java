package org.orgaprop.test7.metrics.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.nio.file.*;

public class MetricsStorage implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsStorage.class);

	private final Path storageDirectory;
	private final StorageIndex index;
	private final BlockingQueue<StorageOperation> operationQueue;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final StorageMetrics metrics;
	private final CompressionManager compressionManager;
	private final MetricsConfig config;
	private final ConfigModule storageConfig;

	public MetricsStorage(Path baseDir) {
		this.config = MetricsConfig.getInstance();
		this.storageConfig = config.getModule("storage");

		if (storageConfig == null) {
			logger.error("Configuration 'storage' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> pathsConfig = (Map<String, Object>) storageConfig.getProperty("paths");
		Map<String, Object> batchConfig = (Map<String, Object>) storageConfig.getProperty("batch");

		this.storageDirectory = baseDir.resolve((String) pathsConfig.get("baseDirectory"));
		this.index = new StorageIndex(storageDirectory.resolve((String) pathsConfig.get("indexFile")));
		this.operationQueue = new LinkedBlockingQueue<>((int) batchConfig.get("queueCapacity"));
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new StorageMetrics();
		this.compressionManager = new CompressionManager();

		initializeStorage();
		startStorageProcessor();
	}

	private void initializeStorage() {
		try {
			Files.createDirectories(storageDirectory);
			loadIndex();
		} catch (Exception e) {
			logger.error("Erreur lors de l'initialisation du stockage", e);
			throw new StorageInitializationException("Échec de l'initialisation", e);
		}
	}

	public void store(String category, String key, byte[] data, Map<String, String> metadata) {
		validateStorage(category, key, data);
		StorageOperation op = new StorageOperation(
				OperationType.STORE,
				category,
				key,
				data,
				metadata);

		if (!operationQueue.offer(op)) {
			handleQueueFull(op);
			return;
		}
		metrics.recordSubmission(category);
	}

	public byte[] retrieve(String category, String key) throws StorageException {
		try {
			StorageEntry entry = index.getEntry(category, key);
			if (entry == null) {
				metrics.recordMiss(category);
				return null;
			}

			byte[] data = Files.readAllBytes(entry.getPath());
			byte[] decompressed = compressionManager.decompress(data);
			metrics.recordHit(category);
			return decompressed;
		} catch (Exception e) {
			metrics.recordError(category);
			throw new StorageException("Erreur de récupération", e);
		}
	}

	private void startStorageProcessor() {
		Map<String, Object> batchConfig = (Map<String, Object>) storageConfig.getProperty("batch");
		Map<String, Object> retentionConfig = (Map<String, Object>) storageConfig.getProperty("retention");

		scheduler.scheduleAtFixedRate(
				this::processBatch,
				0,
				(long) batchConfig.get("flushInterval"),
				TimeUnit.MILLISECONDS);

		if ((boolean) retentionConfig.get("enabled")) {
			scheduler.scheduleAtFixedRate(
					this::performMaintenance,
					(long) retentionConfig.get("cleanupInterval"),
					(long) retentionConfig.get("cleanupInterval"),
					TimeUnit.MILLISECONDS);
		}
	}

	private void processBatch() {
		List<StorageOperation> batch = new ArrayList<>();
		operationQueue.drainTo(batch, BATCH_SIZE);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processBatchOperations(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	private void processBatchOperations(List<StorageOperation> batch) {
		Map<String, Object> compressionConfig = (Map<String, Object>) storageConfig.getProperty("compression");
		boolean useCompression = (boolean) compressionConfig.get("enabled");
		long minSize = (long) compressionConfig.get("minSize");

		for (StorageOperation op : batch) {
			try {
				if (useCompression && op.data.length > minSize) {
					op.data = compressionManager.compress(op.data);
				}
				processOperation(op);
				metrics.recordSuccess(op.category);
			} catch (Exception e) {
				metrics.recordError(op.category);
				logger.error("Erreur traitement opération: {}", e.getMessage());
			}
		}
		saveIndex();
	}

	private void performMaintenance() {
		Map<String, Object> maintenanceConfig = (Map<String, Object>) storageConfig.getProperty("maintenance");
		if ((boolean) maintenanceConfig.get("enabled")) {
			try {
				long maxAge = (long) maintenanceConfig.get("maxFileAge");
				long maxSize = (long) maintenanceConfig.get("maxTotalSize");

				cleanupExpiredData(maxAge);
				enforceStorageLimit(maxSize);
			} catch (Exception e) {
				logger.error("Erreur lors de la maintenance", e);
			}
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				saveIndex();
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
			compressionManager.close();
		}
	}

	private static class StorageMetrics {
		private final Map<String, AtomicInteger> submissionsByCategory = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> hitsByCategory = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> missesByCategory = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorsByCategory = new ConcurrentHashMap<>();
		private final AtomicLong totalProcessed = new AtomicLong(0);
		private final AtomicLong processingTime = new AtomicLong(0);

		void recordSubmission(String category) {
			incrementCounter(submissionsByCategory, category);
		}

		void recordHit(String category) {
			incrementCounter(hitsByCategory, category);
		}

		void recordMiss(String category) {
			incrementCounter(missesByCategory, category);
		}

		void recordError(String category) {
			incrementCounter(errorsByCategory, category);
		}

		void recordBatchProcessing(int size, long duration) {
			totalProcessed.addAndGet(size);
			processingTime.addAndGet(duration);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"submissions", new HashMap<>(submissionsByCategory),
					"hits", new HashMap<>(hitsByCategory),
					"misses", new HashMap<>(missesByCategory),
					"errors", new HashMap<>(errorsByCategory),
					"totalProcessed", totalProcessed.get(),
					"averageProcessingTime", getAverageProcessingTime());
		}

		private double getAverageProcessingTime() {
			long total = totalProcessed.get();
			return total > 0 ? (double) processingTime.get() / total : 0;
		}
	}
}
