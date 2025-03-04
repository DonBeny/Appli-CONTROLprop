package org.orgaprop.test7.metrics.visualization;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChartData {
	private final String title;
	private final Map<String, Double> values;
	private final Map<String, String> labels;
	private final Map<String, Object> metadata;
	private final ChartType type;
	private final ChartStyle style;

	private ChartData(Builder builder) {
		this.title = builder.title;
		this.values = new HashMap<>(builder.values);
		this.labels = new HashMap<>(builder.labels);
		this.metadata = new HashMap<>(builder.metadata);
		this.type = builder.type;
		this.style = builder.style;
	}

	public enum ChartType {
		LINE,
		BAR,
		PIE,
		HEATMAP,
		SCATTER,
		AREA
	}

	public static class ChartStyle {
		private final int[] colors;
		private final float lineWidth;
		private final boolean showLegend;
		private final boolean showGrid;
		private final int backgroundColor;
		private final Map<String, Object> customProperties;

		private ChartStyle(Builder builder) {
			this.colors = builder.colors;
			this.lineWidth = builder.lineWidth;
			this.showLegend = builder.showLegend;
			this.showGrid = builder.showGrid;
			this.backgroundColor = builder.backgroundColor;
			this.customProperties = new HashMap<>(builder.customProperties);
		}

		public static class Builder {
			private int[] colors = new int[] {};
			private float lineWidth = 2.0f;
			private boolean showLegend = true;
			private boolean showGrid = true;
			private int backgroundColor = 0xFFFFFFFF;
			private final Map<String, Object> customProperties = new HashMap<>();

			public Builder setColors(int[] colors) {
				this.colors = colors.clone();
				return this;
			}

			public Builder setLineWidth(float width) {
				this.lineWidth = width;
				return this;
			}

			public Builder setShowLegend(boolean show) {
				this.showLegend = show;
				return this;
			}

			public Builder setShowGrid(boolean show) {
				this.showGrid = show;
				return this;
			}

			public Builder setBackgroundColor(int color) {
				this.backgroundColor = color;
				return this;
			}

			public Builder addCustomProperty(String key, Object value) {
				this.customProperties.put(key, value);
				return this;
			}

			public ChartStyle build() {
				return new ChartStyle(this);
			}
		}
	}

	public static class Builder {
		private String title = "";
		private final Map<String, Double> values = new HashMap<>();
		private final Map<String, String> labels = new HashMap<>();
		private final Map<String, Object> metadata = new HashMap<>();
		private ChartType type = ChartType.LINE;
		private ChartStyle style = new ChartStyle.Builder().build();

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder addValue(String key, Double value) {
			this.values.put(key, value);
			return this;
		}

		public Builder addValues(Map<String, Double> values) {
			this.values.putAll(values);
			return this;
		}

		public Builder addLabel(String key, String label) {
			this.labels.put(key, label);
			return this;
		}

		public Builder addMetadata(String key, Object value) {
			this.metadata.put(key, value);
			return this;
		}

		public Builder setType(ChartType type) {
			this.type = type;
			return this;
		}

		public Builder setStyle(ChartStyle style) {
			this.style = style;
			return this;
		}

		public ChartData build() {
			return new ChartData(this);
		}
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

	public ChartType getType() {
		return type;
	}

	public ChartStyle getStyle() {
		return style;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ChartData chartData = (ChartData) o;
		return Objects.equals(title, chartData.title) &&
				Objects.equals(values, chartData.values) &&
				Objects.equals(labels, chartData.labels) &&
				Objects.equals(metadata, chartData.metadata) &&
				type == chartData.type;
	}

	@Override
	public int hashCode() {
		return Objects.hash(title, values, labels, metadata, type);
	}

	@Override
	public String toString() {
		return String.format("ChartData{title='%s', type=%s, values=%d}",
				title, type, values.size());
	}
}
