package org.orgaprop.test7.metrics.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.NavigableMap;

public class MetricsStatistics {
	private static final Logger logger = LoggerFactory.getLogger(MetricsStatistics.class);

	private final AtomicLong totalSnapshots = new AtomicLong(0);
	private final AtomicLong totalSize = new AtomicLong(0);
	private final Map<Class<?>, TypeStatistics> typeStats;
	private final TimeSeriesData timeSeries;
	private final DistributionAnalyzer distributionAnalyzer;

	public MetricsStatistics() {
		this.typeStats = new ConcurrentHashMap<>();
		this.timeSeries = new TimeSeriesData();
		this.distributionAnalyzer = new DistributionAnalyzer();
	}

	public void recordSnapshot(MetricsSnapshot snapshot) {
		totalSnapshots.incrementAndGet();
		totalSize.addAndGet(snapshot.getCompressedSize());

		Map<String, Object> metrics = snapshot.getMetrics();
		analyzeTypes(metrics);
		timeSeries.addDataPoint(snapshot.getTimestamp(), metrics);
		distributionAnalyzer.analyzeDistribution(metrics);
	}

	private void analyzeTypes(Map<String, Object> metrics) {
		metrics.forEach((key, value) -> {
			if (value != null) {
				TypeStatistics stats = typeStats.computeIfAbsent(
						value.getClass(),
						k -> new TypeStatistics());
				stats.recordValue(value);
			}
		});
	}

	private static class TypeStatistics {
		private final AtomicInteger count = new AtomicInteger(0);
		private final AtomicLong totalSize = new AtomicLong(0);
		private final Set<String> uniqueValues = ConcurrentHashMap.newKeySet();

		void recordValue(Object value) {
			count.incrementAndGet();
			totalSize.addAndGet(estimateSize(value));
			uniqueValues.add(value.toString());
		}

		private long estimateSize(Object value) {
			if (value instanceof String) {
				return 40 + ((String) value).length() * 2L;
			} else if (value instanceof Number) {
				return 16L;
			} else if (value instanceof Map) {
				return 40L + ((Map<?, ?>) value).size() * 32L;
			}
			return 16L;
		}

		Map<String, Object> getStats() {
			return Map.of(
					"count", count.get(),
					"totalSize", totalSize.get(),
					"uniqueValues", uniqueValues.size(),
					"averageSize", count.get() > 0 ? totalSize.get() / count.get() : 0);
		}
	}

	private static class TimeSeriesData {
		private final NavigableMap<Long, MetricsPoint> timePoints = new ConcurrentSkipListMap<>();
		private static final long RETENTION_PERIOD = 7 * 24 * 60 * 60 * 1000L; // 7 jours

		void addDataPoint(long timestamp, Map<String, Object> metrics) {
			timePoints.put(timestamp, new MetricsPoint(metrics));
			cleanup();
		}

		private void cleanup() {
			long cutoff = System.currentTimeMillis() - RETENTION_PERIOD;
			timePoints.headMap(cutoff).clear();
		}

		Map<String, Object> getTimeSeriesStats() {
			Map<String, Object> stats = new HashMap<>();
			stats.putAll(getTimeRange());
			stats.putAll(analyzeTrends());
			return stats;
		}

		private Map<String, Double> getTimeRange() {
			if (timePoints.isEmpty())
				return Map.of();
			return Map.of(
					"startTime", (double) timePoints.firstKey(),
					"endTime", (double) timePoints.lastKey());
		}

		private Map<String, Object> analyzeTrends() {
			return Map.of(
					"growthRate", calculateGrowthRate(),
					"volatility", calculateVolatility(),
					"seasonality", detectSeasonality());
		}
	}

	private static class DistributionAnalyzer {
		private final Map<String, ValueDistribution> distributions = new ConcurrentHashMap<>();

		void analyzeDistribution(Map<String, Object> metrics) {
			metrics.forEach((key, value) -> {
				if (value instanceof Number) {
					distributions.computeIfAbsent(key, k -> new ValueDistribution())
							.addValue(((Number) value).doubleValue());
				}
			});
		}

		Map<String, Object> getDistributionStats() {
			Map<String, Object> stats = new HashMap<>();
			distributions.forEach((key, dist) -> stats.put(key, dist.getStats()));
			return stats;
		}
	}

	public Map<String, Object> generateReport() {
		Map<String, Object> report = new HashMap<>();
		report.put("totalSnapshots", totalSnapshots.get());
		report.put("totalSize", totalSize.get());
		report.put("typeStatistics", getTypeStatistics());
		report.put("timeSeriesAnalysis", timeSeries.getTimeSeriesStats());
		report.put("distributions", distributionAnalyzer.getDistributionStats());
		return report;
	}
}
