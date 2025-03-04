package org.orgaprop.test7.metrics.visualization;

import java.util.HashMap;
import java.util.Map;

public class VisualizationFormat {
	private final FormatType type;
	private final Map<String, Object> formatOptions;
	private final DataLayout dataLayout;
	private final ScaleConfig scales;
	private final LegendConfig legend;

	private VisualizationFormat(Builder builder) {
		this.type = builder.type;
		this.formatOptions = new HashMap<>(builder.formatOptions);
		this.dataLayout = builder.dataLayout;
		this.scales = builder.scales;
		this.legend = builder.legend;
	}

	public enum FormatType {
		LINE,
		BAR,
		PIE,
		HEATMAP,
		SCATTER,
		AREA,
		CUSTOM
	}

	public static class DataLayout {
		private final AxisConfig xAxis;
		private final AxisConfig yAxis;
		private final boolean stacked;
		private final boolean normalized;

		private DataLayout(Builder builder) {
			this.xAxis = builder.xAxis;
			this.yAxis = builder.yAxis;
			this.stacked = builder.stacked;
			this.normalized = builder.normalized;
		}

		public static class Builder {
			private AxisConfig xAxis = new AxisConfig.Builder().build();
			private AxisConfig yAxis = new AxisConfig.Builder().build();
			private boolean stacked = false;
			private boolean normalized = false;

			public Builder setXAxis(AxisConfig xAxis) {
				this.xAxis = xAxis;
				return this;
			}

			public Builder setYAxis(AxisConfig yAxis) {
				this.yAxis = yAxis;
				return this;
			}

			public Builder setStacked(boolean stacked) {
				this.stacked = stacked;
				return this;
			}

			public Builder setNormalized(boolean normalized) {
				this.normalized = normalized;
				return this;
			}

			public DataLayout build() {
				return new DataLayout(this);
			}
		}
	}

	public static class AxisConfig {
		private final String label;
		private final boolean logarithmic;
		private final double min;
		private final double max;

		private AxisConfig(Builder builder) {
			this.label = builder.label;
			this.logarithmic = builder.logarithmic;
			this.min = builder.min;
			this.max = builder.max;
		}

		public static class Builder {
			private String label = "";
			private boolean logarithmic = false;
			private double min = Double.NaN;
			private double max = Double.NaN;

			public Builder setLabel(String label) {
				this.label = label;
				return this;
			}

			public Builder setLogarithmic(boolean logarithmic) {
				this.logarithmic = logarithmic;
				return this;
			}

			public Builder setRange(double min, double max) {
				this.min = min;
				this.max = max;
				return this;
			}

			public AxisConfig build() {
				return new AxisConfig(this);
			}
		}
	}

	public static class ScaleConfig {
		private final boolean autoScale;
		private final double domainMin;
		private final double domainMax;
		private final double rangeMin;
		private final double rangeMax;

		private ScaleConfig(Builder builder) {
			this.autoScale = builder.autoScale;
			this.domainMin = builder.domainMin;
			this.domainMax = builder.domainMax;
			this.rangeMin = builder.rangeMin;
			this.rangeMax = builder.rangeMax;
		}

		public static class Builder {
			private boolean autoScale = true;
			private double domainMin = 0.0;
			private double domainMax = 1.0;
			private double rangeMin = 0.0;
			private double rangeMax = 1.0;

			public Builder setAutoScale(boolean autoScale) {
				this.autoScale = autoScale;
				return this;
			}

			public Builder setDomain(double min, double max) {
				this.domainMin = min;
				this.domainMax = max;
				return this;
			}

			public Builder setRange(double min, double max) {
				this.rangeMin = min;
				this.rangeMax = max;
				return this;
			}

			public ScaleConfig build() {
				return new ScaleConfig(this);
			}
		}
	}

	public static class LegendConfig {
		private final boolean visible;
		private final Position position;
		private final Map<String, String> labels;

		private LegendConfig(Builder builder) {
			this.visible = builder.visible;
			this.position = builder.position;
			this.labels = new HashMap<>(builder.labels);
		}

		public enum Position {
			TOP, RIGHT, BOTTOM, LEFT
		}

		public static class Builder {
			private boolean visible = true;
			private Position position = Position.RIGHT;
			private final Map<String, String> labels = new HashMap<>();

			public Builder setVisible(boolean visible) {
				this.visible = visible;
				return this;
			}

			public Builder setPosition(Position position) {
				this.position = position;
				return this;
			}

			public Builder addLabel(String key, String label) {
				this.labels.put(key, label);
				return this;
			}

			public LegendConfig build() {
				return new LegendConfig(this);
			}
		}
	}

	public static class Builder {
		private FormatType type = FormatType.LINE;
		private final Map<String, Object> formatOptions = new HashMap<>();
		private DataLayout dataLayout = new DataLayout.Builder().build();
		private ScaleConfig scales = new ScaleConfig.Builder().build();
		private LegendConfig legend = new LegendConfig.Builder().build();

		public Builder setType(FormatType type) {
			this.type = type;
			return this;
		}

		public Builder addFormatOption(String key, Object value) {
			this.formatOptions.put(key, value);
			return this;
		}

		public Builder setDataLayout(DataLayout layout) {
			this.dataLayout = layout;
			return this;
		}

		public Builder setScales(ScaleConfig scales) {
			this.scales = scales;
			return this;
		}

		public Builder setLegend(LegendConfig legend) {
			this.legend = legend;
			return this;
		}

		public VisualizationFormat build() {
			return new VisualizationFormat(this);
		}
	}
}
