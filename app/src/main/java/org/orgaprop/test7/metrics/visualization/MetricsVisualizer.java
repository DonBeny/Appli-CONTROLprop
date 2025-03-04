package org.orgaprop.test7.metrics.visualization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MetricsVisualizer implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsVisualizer.class);

	private final MetricsCoordinator coordinator;
	private final Map<String, DataTransformer> transformers;
	private final ExecutorService executor;
	private final AtomicBoolean isRunning;
	private final VisualizationMetrics metrics;
	private final MetricsConfig config;
	private final ConfigModule visualConfig;

	public MetricsVisualizer(MetricsCoordinator coordinator) {
		this.config = MetricsConfig.getInstance();
		this.visualConfig = config.getModule("visualization");

		if (visualConfig == null) {
			logger.error("Configuration 'visualization' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		this.coordinator = coordinator;
		this.transformers = new ConcurrentHashMap<>();

		Map<String, Object> perfConfig = (Map<String, Object>) visualConfig.getProperty("performance");
		this.executor = Executors.newFixedThreadPool((int) perfConfig.get("threadPoolSize"));

		this.isRunning = new AtomicBoolean(true);
		this.metrics = new VisualizationMetrics();

		initializeTransformers();
	}

	public ChartData prepareChartData(String metricType, Map<String, Object> rawData,
			VisualizationFormat format) {
		try {
			DataTransformer transformer = transformers.get(format.name());
			if (transformer == null) {
				throw new IllegalArgumentException("Format non supporté: " + format);
			}

			ChartData chartData = transformer.transform(metricType, rawData);
			metrics.recordTransformation(format.name(), true);
			return chartData;
		} catch (Exception e) {
			metrics.recordTransformation(format.name(), false);
			throw new VisualizationException("Erreur de transformation", e);
		}
	}

	private void initializeTransformers() {
		Map<String, Object> transformerConfig = (Map<String, Object>) visualConfig.getProperty("transformers");
		List<String> enabledTypes = (List<String>) transformerConfig.get("enabledTypes");

		for (String type : enabledTypes) {
			switch (type) {
				case "LINE":
					registerTransformer(VisualizationFormat.LINE, new LineChartTransformer());
					break;
				case "BAR":
					registerTransformer(VisualizationFormat.BAR, new BarChartTransformer());
					break;
				case "PIE":
					registerTransformer(VisualizationFormat.PIE, new PieChartTransformer());
					break;
				case "HEATMAP":
					registerTransformer(VisualizationFormat.HEATMAP, new HeatmapTransformer());
					break;
			}
		}
	}

	public void registerTransformer(VisualizationFormat format, DataTransformer transformer) {
		transformers.put(format.name(), transformer);
		metrics.recordTransformerRegistration(format.name());
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			executor.shutdown();
			try {
				Map<String, Object> perfConfig = (Map<String, Object>) visualConfig.getProperty("performance");
				long updateInterval = (long) perfConfig.get("updateInterval");

				if (!executor.awaitTermination(updateInterval, TimeUnit.MILLISECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				executor.shutdownNow();
			}
		}
	}

	public interface DataTransformer {
		ChartData transform(String metricType, Map<String, Object> rawData);
	}

	public static class ChartData {
		private final String title;
		private final Map<String, Double> values;
		private final Map<String, String> labels;
		private final Map<String, Object> metadata;

		public ChartData(String title, Map<String, Double> values,
				Map<String, String> labels, Map<String, Object> metadata) {
			this.title = title;
			this.values = new HashMap<>(values);
			this.labels = new HashMap<>(labels);
			this.metadata = new HashMap<>(metadata);
		}

		// Getters
		public String getTitle() {
			return title;
		}

		public Map<String, Double> getValues() {
			return new HashMap<>(values);
		}

		public Map<String, String> getLabels() {
			return new HashMap<>(labels);
		}

		public Map<String, Object> getMetadata() {
			return new HashMap<>(metadata);
		}
	}

	public enum VisualizationFormat {
		LINE,
		BAR,
		PIE,
		HEATMAP
	}

	private static class VisualizationMetrics {
		private final Map<String, AtomicInteger> transformationsByFormat = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorsByFormat = new ConcurrentHashMap<>();
		private final AtomicInteger totalTransformers = new AtomicInteger(0);

		void recordTransformerRegistration(String format) {
			totalTransformers.incrementAndGet();
		}

		void recordTransformation(String format, boolean success) {
			if (success) {
				incrementCounter(transformationsByFormat, format);
			} else {
				incrementCounter(errorsByFormat, format);
			}
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalTransformers", totalTransformers.get(),
					"transformations", new HashMap<>(transformationsByFormat),
					"errors", new HashMap<>(errorsByFormat));
		}
	}
}
