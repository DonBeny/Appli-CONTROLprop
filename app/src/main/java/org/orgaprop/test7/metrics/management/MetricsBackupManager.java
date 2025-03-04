package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.HashMap;

public class MetricsBackupManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsBackupManager.class);

	private static final int MAX_BACKUPS = 10;
	private static final long DEFAULT_BACKUP_INTERVAL = 30 * 60 * 1000; // 30 minutes
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final Path backupDir;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final AtomicInteger backupFailureCount;
	private final BackupValidator validator;
	private final BackupCompressor compressor;
	private final BackupRotationStrategy rotationStrategy;
	private final BackupMetrics metrics;
	private ScheduledFuture<?> backupTask;

	public MetricsBackupManager(Path backupDir) {
		this.backupDir = backupDir;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "backup-worker");
			t.setDaemon(true);
			return t;
		});
		this.isRunning = new AtomicBoolean(false);
		this.backupFailureCount = new AtomicInteger(0);
		this.validator = new BackupValidator();
		this.compressor = new BackupCompressor();
		this.rotationStrategy = new BackupRotationStrategy(MAX_BACKUPS);
		this.metrics = new BackupMetrics();

		initializeBackupDirectory();
	}

	private void initializeBackupDirectory() {
		try {
			Files.createDirectories(backupDir);
			logger.info("Répertoire de backup initialisé : {}", backupDir);
		} catch (Exception e) {
			logger.error("Impossible de créer le répertoire de backup", e);
			throw new BackupInitializationException("Erreur initialisation backup", e);
		}
	}

	public void scheduleBackup() {
		scheduleBackup(DEFAULT_BACKUP_INTERVAL);
	}

	public void scheduleBackup(long interval) {
		if (backupTask != null && !backupTask.isDone()) {
			logger.warn("Tâche de backup déjà programmée");
			return;
		}

		backupTask = scheduler.scheduleAtFixedRate(
				() -> performBackupSafely(),
				interval,
				interval,
				TimeUnit.MILLISECONDS);

		logger.info("Backup automatique programmé avec intervalle de {} ms", interval);
	}

	public void performFinalBackup() {
		if (!isRunning.compareAndSet(false, true)) {
			logger.warn("Backup final impossible : déjà en cours");
			return;
		}

		try {
			logger.info("Début du backup final");
			performBackup(true);
		} catch (Exception e) {
			logger.error("Erreur pendant le backup final", e);
			metrics.recordFailure();
		} finally {
			isRunning.set(false);
		}
	}

	private void performBackupSafely() {
		if (!isRunning.compareAndSet(false, true)) {
			return;
		}

		try {
			performBackup(false);
		} catch (Exception e) {
			handleBackupError(e);
		} finally {
			isRunning.set(false);
		}
	}

	private void performBackup(boolean isFinal) throws Exception {
		long startTime = System.nanoTime();

		// Récupération et compression des métriques
		Map<String, Object> metrics = getMetricsSnapshot();
		byte[] compressed = compressor.compress(metrics);

		// Création du fichier de backup
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		Path backupFile = backupDir.resolve("metrics_" + timestamp + ".gz");

		// Écriture atomique
		writeBackupFile(backupFile, compressed);

		// Validation
		if (validator.validate(backupFile, metrics)) {
			rotationStrategy.rotate(backupDir);
			backupFailureCount.set(0);

			long duration = (System.nanoTime() - startTime) / 1_000_000; // en ms
			this.metrics.recordBackup(compressed.length, duration);

			logger.info("Backup {} réussi : {}", isFinal ? "final" : "périodique", backupFile);
		} else {
			throw new BackupValidationException("Échec de validation du backup");
		}
	}

	private void writeBackupFile(Path file, byte[] data) throws Exception {
		Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

		try {
			Files.write(tempFile, data, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING);
			Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	private void handleBackupError(Exception e) {
		logger.error("Erreur pendant le backup", e);
		metrics.recordFailure();

		int failures = backupFailureCount.incrementAndGet();
		if (failures >= MAX_RETRY_ATTEMPTS) {
			logger.error("Trop d'échecs de backup consécutifs");
			// Notification possible du système de monitoring
		}
	}

	public Map<String, Object> getMetrics() {
		return metrics.getStats();
	}

	@Override
	public void close() {
		try {
			performFinalBackup();
		} catch (Exception e) {
			logger.error("Erreur lors du backup final", e);
		} finally {
			if (backupTask != null) {
				backupTask.cancel(false);
			}
			scheduler.shutdown();
			try {
				scheduler.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
		}
	}

	private static class BackupMetrics {
		private final AtomicInteger totalBackups = new AtomicInteger(0);
		private final AtomicLong totalSize = new AtomicLong(0);
		private final AtomicInteger failureCount = new AtomicInteger(0);
		private final AtomicLong totalDuration = new AtomicLong(0);

		void recordBackup(long size, long duration) {
			totalBackups.incrementAndGet();
			totalSize.addAndGet(size);
			totalDuration.addAndGet(duration);
		}

		void recordFailure() {
			failureCount.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalBackups", totalBackups.get(),
					"totalSize", totalSize.get(),
					"failureCount", failureCount.get(),
					"averageDuration", getAverageDuration(),
					"averageSize", getAverageSize());
		}

		private double getAverageDuration() {
			int backups = totalBackups.get();
			return backups > 0 ? (double) totalDuration.get() / backups : 0;
		}

		private double getAverageSize() {
			int backups = totalBackups.get();
			return backups > 0 ? (double) totalSize.get() / backups : 0;
		}
	}
}
