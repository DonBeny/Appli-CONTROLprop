package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.NavigableMap;

public class StatisticsManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsManager.class);

	private final ConcurrentNavigableMap<Long, MetricPoint> timeSeriesData;
	private final Map<String, StatisticalSummary> summaries;
	private final AtomicLong totalSamples;
	private final SlidingWindowCounter slidingWindow;
	private final DescriptiveStats descriptiveStats;
	private final long retentionPeriod;

	public StatisticsManager(long retentionPeriod) {
		this.timeSeriesData = new ConcurrentSkipListMap<>();
		this.summaries = new ConcurrentHashMap<>();
		this.totalSamples = new AtomicLong(0);
		this.slidingWindow = new SlidingWindowCounter();
		this.descriptiveStats = new DescriptiveStats();
		this.retentionPeriod = retentionPeriod;
	}

	public void recordValue(String metric, double value, long timestamp) {
		try {
			MetricPoint point = new MetricPoint(value, timestamp);
			timeSeriesData.put(timestamp, point);
			updateStatistics(metric, value);
			cleanupOldData();
		} catch (Exception e) {
			logger.error("Erreur lors de l'enregistrement de la valeur", e);
		}
	}

	private void updateStatistics(String metric, double value) {
		StatisticalSummary summary = summaries.computeIfAbsent(metric, k -> new StatisticalSummary());
		summary.addValue(value);
		totalSamples.incrementAndGet();
		slidingWindow.increment(metric);
		descriptiveStats.update(value);
	}

	private void cleanupOldData() {
		long cutoffTime = System.currentTimeMillis() - retentionPeriod;
		timeSeriesData.headMap(cutoffTime).clear();
	}

	public Map<String, Object> getStats(String metric) {
		StatisticalSummary summary = summaries.get(metric);
		if (summary == null) {
			return Map.of();
		}
		return Map.of(
				"count", summary.getCount(),
				"mean", summary.getMean(),
				"min", summary.getMin(),
				"max", summary.getMax(),
				"stdDev", summary.getStandardDeviation(),
				"recentCount", slidingWindow.getCount(metric),
				"descriptiveStats", descriptiveStats.getStats());
	}

	private static class MetricPoint {
		private final double value;
		private final long timestamp;

		MetricPoint(double value, long timestamp) {
			this.value = value;
			this.timestamp = timestamp;
		}
	}

	private static class StatisticalSummary {
		private final AtomicLong count = new AtomicLong(0);
		private final AtomicDouble sum = new AtomicDouble(0);
		private final AtomicDouble sumSquares = new AtomicDouble(0);
		private final AtomicDouble min = new AtomicDouble(Double.MAX_VALUE);
		private final AtomicDouble max = new AtomicDouble(Double.MIN_VALUE);

		void addValue(double value) {
			count.incrementAndGet();
			sum.addAndGet(value);
			sumSquares.addAndGet(value * value);
			updateMin(value);
			updateMax(value);
		}

		private void updateMin(double value) {
			min.updateAndGet(current -> Math.min(current, value));
		}

		private void updateMax(double value) {
			max.updateAndGet(current -> Math.max(current, value));
		}

		double getMean() {
			long n = count.get();
			return n > 0 ? sum.get() / n : 0.0;
		}

		double getStandardDeviation() {
			long n = count.get();
			if (n < 2)
				return 0.0;
			double mean = getMean();
			return Math.sqrt((sumSquares.get() / n) - (mean * mean));
		}

		long getCount() {
			return count.get();
		}

		double getMin() {
			return min.get();
		}

		double getMax() {
			return max.get();
		}
	}

	@Override
	public void close() {
		timeSeriesData.clear();
		summaries.clear();
		slidingWindow.reset();
		descriptiveStats.reset();
	}
}
