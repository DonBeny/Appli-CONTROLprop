package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MonitoringManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MonitoringManager.class);

	private static final long CHECK_INTERVAL = 10_000; // 10 secondes
	private static final int MAX_HISTORY_SIZE = 1000;

	private final Map<String, MetricCollector> collectors;
	private final ConcurrentNavigableMap<Long, SystemSnapshot> monitoringHistory;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final MonitoringMetrics metrics;
	private final AlertManager alertManager;
	private final ThresholdManager thresholdManager;

	public MonitoringManager(AlertManager alertManager) {
		this.collectors = new ConcurrentHashMap<>();
		this.monitoringHistory = new ConcurrentSkipListMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new MonitoringMetrics();
		this.alertManager = alertManager;
		this.thresholdManager = new ThresholdManager();

		startMonitoring();
	}

	public void registerCollector(String collectorId, MetricCollector collector) {
		collectors.put(collectorId, collector);
		metrics.recordCollectorRegistration(collectorId);
	}

	private void startMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::collectMetrics,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void collectMetrics() {
		try {
			SystemSnapshot snapshot = captureSystemState();
			analyzeSnapshot(snapshot);
			updateHistory(snapshot);
			cleanupHistory();
		} catch (Exception e) {
			handleCollectionError(e);
		}
	}

	private SystemSnapshot captureSystemState() {
		Map<String, MetricData> collectedData = new HashMap<>();
		collectors.forEach((id, collector) -> {
			try {
				collectedData.put(id, collector.collect());
			} catch (Exception e) {
				handleCollectorError(id, e);
			}
		});
		return new SystemSnapshot(collectedData, System.currentTimeMillis());
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
		}
	}

	public interface MetricCollector {
		MetricData collect();
	}

	private static class SystemSnapshot {
		private final Map<String, MetricData> metrics;
		private final long timestamp;

		SystemSnapshot(Map<String, MetricData> metrics, long timestamp) {
			this.metrics = new HashMap<>(metrics);
			this.timestamp = timestamp;
		}
	}

	private static class MonitoringMetrics {
		private final Map<String, AtomicInteger> collectionsByCollector = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorsByCollector = new ConcurrentHashMap<>();
		private final AtomicInteger totalCollectors = new AtomicInteger(0);
		private final AtomicLong totalCollections = new AtomicLong(0);

		void recordCollectorRegistration(String collectorId) {
			totalCollectors.incrementAndGet();
		}

		void recordCollection(String collectorId) {
			incrementCounter(collectionsByCollector, collectorId);
			totalCollections.incrementAndGet();
		}

		void recordError(String collectorId) {
			incrementCounter(errorsByCollector, collectorId);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalCollectors", totalCollectors.get(),
					"totalCollections", totalCollections.get(),
					"collectionsByCollector", new HashMap<>(collectionsByCollector),
					"errorsByCollector", new HashMap<>(errorsByCollector));
		}
	}
}
