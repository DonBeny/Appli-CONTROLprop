package org.orgaprop.test7.metrics.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsCalculator {
	private static final Logger logger = LoggerFactory.getLogger(StatisticsCalculator.class);

	private final Map<String, MetricStats> statsMap;
	private final AtomicLong totalSamplesProcessed;
	private final RollingWindow rollingWindow;

	public StatisticsCalculator() {
		this.statsMap = new ConcurrentHashMap<>();
		this.totalSamplesProcessed = new AtomicLong(0);
		this.rollingWindow = new RollingWindow();
	}

	public void addValue(String metric, double value) {
		try {
			MetricStats stats = statsMap.computeIfAbsent(metric, k -> new MetricStats());
			stats.addValue(value);
			rollingWindow.addValue(metric, value);
			totalSamplesProcessed.incrementAndGet();
		} catch (Exception e) {
			logger.error("Erreur lors de l'ajout de la valeur pour {}: {}", metric, e.getMessage());
		}
	}

	public Map<String, Object> getStats(String metric) {
		MetricStats stats = statsMap.get(metric);
		if (stats == null) {
			return Map.of();
		}
		return stats.getStats();
	}

	public Map<String, Object> getAllStats() {
		Map<String, Object> allStats = new HashMap<>();
		statsMap.forEach((metric, stats) -> allStats.put(metric, stats.getStats()));
		allStats.put("totalSamples", totalSamplesProcessed.get());
		allStats.put("rollingStats", rollingWindow.getStats());
		return allStats;
	}

	private static class MetricStats {
		private final AtomicLong count = new AtomicLong(0);
		private double min = Double.MAX_VALUE;
		private double max = Double.MIN_VALUE;
		private double sum = 0.0;
		private double sumOfSquares = 0.0;

		synchronized void addValue(double value) {
			count.incrementAndGet();
			sum += value;
			sumOfSquares += value * value;
			min = Math.min(min, value);
			max = Math.max(max, value);
		}

		Map<String, Object> getStats() {
			long n = count.get();
			if (n == 0) {
				return Map.of("count", 0);
			}

			double mean = sum / n;
			double variance = (sumOfSquares / n) - (mean * mean);
			double stdDev = Math.sqrt(Math.max(0, variance));

			return Map.of(
					"count", n,
					"min", min,
					"max", max,
					"mean", mean,
					"stdDev", stdDev,
					"variance", variance);
		}
	}

	private static class RollingWindow {
		private static final int WINDOW_SIZE = 100;
		private final Map<String, CircularBuffer> buffers = new ConcurrentHashMap<>();

		void addValue(String metric, double value) {
			buffers.computeIfAbsent(metric, k -> new CircularBuffer(WINDOW_SIZE))
					.add(value);
		}

		Map<String, Object> getStats() {
			Map<String, Object> stats = new HashMap<>();
			buffers.forEach((metric, buffer) -> stats.put(metric, buffer.getStats()));
			return stats;
		}
	}

	private static class CircularBuffer {
		private final double[] values;
		private int currentIndex = 0;
		private int size = 0;
		private final int capacity;

		CircularBuffer(int capacity) {
			this.capacity = capacity;
			this.values = new double[capacity];
		}

		synchronized void add(double value) {
			values[currentIndex] = value;
			currentIndex = (currentIndex + 1) % capacity;
			if (size < capacity)
				size++;
		}

		Map<String, Object> getStats() {
			if (size == 0)
				return Map.of();

			double sum = 0;
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;

			for (int i = 0; i < size; i++) {
				double value = values[i];
				sum += value;
				min = Math.min(min, value);
				max = Math.max(max, value);
			}

			double mean = sum / size;
			return Map.of(
					"windowSize", size,
					"rollingMean", mean,
					"rollingMin", min,
					"rollingMax", max);
		}
	}
}
