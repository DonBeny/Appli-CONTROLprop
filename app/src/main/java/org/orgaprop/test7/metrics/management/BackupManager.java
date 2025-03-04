package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.nio.file.*;

public class BackupManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(BackupManager.class);

	private final ScheduledExecutorService scheduler;
	private final Path backupDirectory;
	private final MetricsConfig config;
	private final ConfigModule managementConfig;

	public BackupManager() {
		this.config = MetricsConfig.getInstance();
		this.managementConfig = config.getModule("management");

		if (managementConfig == null) {
			logger.error("Configuration 'management' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> backupConfig = (Map<String, Object>) managementConfig.getProperty("backup");
		this.backupDirectory = Paths.get((String) backupConfig.get("backupLocation"));
		this.scheduler = Executors.newScheduledThreadPool(1);

		initialize();
	}

	private void initialize() {
		Map<String, Object> backupConfig = (Map<String, Object>) managementConfig.getProperty("backup");
		long interval = (long) backupConfig.get("interval");

		scheduler.scheduleAtFixedRate(
				this::performBackup,
				interval,
				interval,
				TimeUnit.MILLISECONDS);
	}

	public void performBackup() {
		if (!isRunning.get())
			return;

		try {
			long startTime = System.nanoTime();
			Map<String, Object> metricsData = collectMetricsData();
			String backupFileName = generateBackupFileName();
			saveBackup(backupFileName, metricsData);
			cleanupOldBackups();
			metrics.recordBackup(System.nanoTime() - startTime);
		} catch (Exception e) {
			handleBackupError(e);
		}
	}

	private void saveBackup(String fileName, Map<String, Object> data) {
		Path backupFile = backupDirectory.resolve(fileName);
		Path tempFile = backupFile.resolveSibling(fileName + ".tmp");

		try {
			byte[] compressed = compressionManager.compress(serializeData(data));
			Files.write(tempFile, compressed, StandardOpenOption.CREATE_NEW);
			Files.move(tempFile, backupFile, StandardCopyOption.ATOMIC_MOVE);
			metrics.recordBackupSize(compressed.length);
			logger.info("Sauvegarde créée : {}", fileName);
		} catch (Exception e) {
			handleSaveError(e, tempFile);
		}
	}

	private void cleanupOldBackups() {
		try {
			Files.list(backupDirectory)
					.filter(this::isBackupFile)
					.sorted((p1, p2) -> -p1.getFileName().toString().compareTo(p2.getFileName().toString()))
					.skip(MAX_BACKUP_FILES)
					.forEach(this::deleteBackupFile);
		} catch (Exception e) {
			logger.error("Erreur lors du nettoyage des anciennes sauvegardes", e);
		}
	}

	private void deleteBackupFile(Path file) {
		try {
			Files.deleteIfExists(file);
			metrics.recordDeletion();
		} catch (Exception e) {
			logger.error("Erreur lors de la suppression du fichier {}", file, e);
		}
	}

	private boolean isBackupFile(Path path) {
		String fileName = path.getFileName().toString();
		return fileName.startsWith(BACKUP_FILE_PREFIX) &&
				fileName.endsWith(BACKUP_FILE_EXTENSION);
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
			compressionManager.close();
		}
	}

	private static class BackupMetrics {
		private final AtomicLong totalBackups = new AtomicLong(0);
		private final AtomicLong totalBackupTime = new AtomicLong(0);
		private final AtomicLong totalBackupSize = new AtomicLong(0);
		private final AtomicLong backupErrors = new AtomicLong(0);
		private final AtomicLong deletedBackups = new AtomicLong(0);

		void recordBackup(long duration) {
			totalBackups.incrementAndGet();
			totalBackupTime.addAndGet(duration);
		}

		void recordBackupSize(long size) {
			totalBackupSize.addAndGet(size);
		}

		void recordError() {
			backupErrors.incrementAndGet();
		}

		void recordDeletion() {
			deletedBackups.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalBackups", totalBackups.get(),
					"averageBackupTime", getAverageBackupTime(),
					"totalBackupSize", totalBackupSize.get(),
					"errors", backupErrors.get(),
					"deletedBackups", deletedBackups.get());
		}

		private double getAverageBackupTime() {
			long backups = totalBackups.get();
			return backups > 0 ? (double) totalBackupTime.get() / backups : 0;
		}
	}
}
