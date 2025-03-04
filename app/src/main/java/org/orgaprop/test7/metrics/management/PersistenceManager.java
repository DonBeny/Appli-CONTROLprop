package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.io.*;
import java.nio.file.*;

public class PersistenceManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(PersistenceManager.class);

	private static final int BATCH_SIZE = 1000;
	private static final long FLUSH_INTERVAL = 30_000; // 30 secondes
	private static final int MAX_RETRY_ATTEMPTS = 3;
	private static final String FILE_EXTENSION = ".dat";

	private final BlockingQueue<PersistenceOperation> operationQueue;
	private final ScheduledExecutorService scheduler;
	private final CompressionManager compressionManager;
	private final Path storageDirectory;
	private final PersistenceMetrics metrics;
	private final AtomicBoolean isRunning;
	private final RetentionPolicy retentionPolicy;
	private final AlertManager alertManager;

	public PersistenceManager(Path storageDirectory, RetentionPolicy retentionPolicy, AlertManager alertManager) {
		this.operationQueue = new LinkedBlockingQueue<>();
		this.scheduler = createScheduler();
		this.compressionManager = new CompressionManager();
		this.storageDirectory = storageDirectory;
		this.metrics = new PersistenceMetrics();
		this.isRunning = new AtomicBoolean(true);
		this.retentionPolicy = retentionPolicy;
		this.alertManager = alertManager;

		initializeStorage();
		startPeriodicOperations();
	}

	private void initializeStorage() {
		try {
			Files.createDirectories(storageDirectory);
			logger.info("Répertoire de stockage initialisé : {}", storageDirectory);
		} catch (IOException e) {
			logger.error("Impossible de créer le répertoire de stockage", e);
			throw new StorageInitializationException("Erreur initialisation stockage", e);
		}
	}

	public void store(String key, byte[] data, StorageOptions options) {
		if (!isRunning.get()) {
			throw new IllegalStateException("PersistenceManager est arrêté");
		}

		PersistenceOperation operation = new PersistenceOperation(
				OperationType.STORE,
				key,
				data,
				options);

		if (!operationQueue.offer(operation)) {
			handleQueueFull(operation);
			return;
		}
		metrics.recordOperationQueued(OperationType.STORE);
	}

	public byte[] retrieve(String key) throws IOException {
		Path filePath = resolveFilePath(key);
		try {
			if (!Files.exists(filePath)) {
				metrics.recordMiss();
				return null;
			}

			byte[] compressed = Files.readAllBytes(filePath);
			byte[] data = compressionManager.decompress(compressed);
			metrics.recordHit();
			return data;
		} catch (Exception e) {
			handleRetrieveError(e, key);
			throw new IOException("Erreur lors de la récupération: " + key, e);
		}
	}

	private void startPeriodicOperations() {
		scheduler.scheduleAtFixedRate(
				this::processOperationBatch,
				FLUSH_INTERVAL,
				FLUSH_INTERVAL,
				TimeUnit.MILLISECONDS);

		scheduler.scheduleAtFixedRate(
				this::applyRetentionPolicy,
				retentionPolicy.getCheckInterval(),
				retentionPolicy.getCheckInterval(),
				TimeUnit.MILLISECONDS);
	}

	private void processOperationBatch() {
		List<PersistenceOperation> batch = new ArrayList<>();
		operationQueue.drainTo(batch, BATCH_SIZE);

		if (!batch.isEmpty()) {
			processBatch(batch);
		}
	}

	private void processBatch(List<PersistenceOperation> batch) {
		long startTime = System.nanoTime();
		Map<OperationType, List<PersistenceOperation>> operationsByType = batch.stream().collect(groupingBy(op -> op.type));

		operationsByType.forEach(this::processOperationsOfType);

		metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
	}

	private void processOperationsOfType(OperationType type, List<PersistenceOperation> operations) {
		switch (type) {
			case STORE:
				processStoreOperations(operations);
				break;
			case DELETE:
				processDeleteOperations(operations);
				break;
			default:
				logger.warn("Type d'opération non supporté: {}", type);
		}
	}

	private void processStoreOperations(List<PersistenceOperation> operations) {
		operations.forEach(op -> {
			try {
				storeWithRetry(op.key, op.data, op.options);
				metrics.recordSuccess(OperationType.STORE);
			} catch (Exception e) {
				handleStoreError(e, op);
			}
		});
	}

	private void applyRetentionPolicy() {
		try {
			List<Path> filesToDelete = retentionPolicy.getFilesToDelete(storageDirectory);
			for (Path file : filesToDelete) {
				Files.deleteIfExists(file);
				metrics.recordFileDeleted();
			}
		} catch (Exception e) {
			logger.error("Erreur lors de l'application de la politique de rétention", e);
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				processRemainingOperations();
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

	// Classes internes et énumérations
	private enum OperationType {
		STORE, DELETE
	}

	private static class PersistenceOperation {
		final OperationType type;
		final String key;
		final byte[] data;
		final StorageOptions options;

		PersistenceOperation(OperationType type, String key, byte[] data, StorageOptions options) {
			this.type = type;
			this.key = key;
			this.data = data;
			this.options = options;
		}
	}

	private static class StorageOptions {
		final boolean compress;
		final RetentionPolicy retentionPolicy;
		final Map<String, String> metadata;

		StorageOptions(boolean compress, RetentionPolicy retentionPolicy, Map<String, String> metadata) {
			this.compress = compress;
			this.retentionPolicy = retentionPolicy;
			this.metadata = metadata;
		}
	}

	private static class PersistenceMetrics {
		private final AtomicLong queuedRecords = new AtomicLong(0);
		private final AtomicLong processedRecords = new AtomicLong(0);
		private final AtomicLong totalProcessingTime = new AtomicLong(0);
		private final AtomicLong failedWrites = new AtomicLong(0);
		private final AtomicLong deletedFiles = new AtomicLong(0);

		void recordQueued() {
			queuedRecords.incrementAndGet();
		}

		void recordBatchProcessed(int size, long duration) {
			processedRecords.addAndGet(size);
			totalProcessingTime.addAndGet(duration);
		}

		void recordFailure() {
			failedWrites.incrementAndGet();
		}

		void recordFileDeleted() {
			deletedFiles.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"queuedRecords", queuedRecords.get(),
					"processedRecords", processedRecords.get(),
					"averageProcessingTime", getAverageProcessingTime(),
					"failedWrites", failedWrites.get(),
					"deletedFiles", deletedFiles.get());
		}

		private double getAverageProcessingTime() {
			long processed = processedRecords.get();
			return processed > 0 ? (double) totalProcessingTime.get() / processed : 0;
		}
	}
}
