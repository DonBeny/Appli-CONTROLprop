package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.io.*;
import java.util.zip.*;

public class CompressionManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsCompressionManager.class);

	private static final int BUFFER_SIZE = 8192;
	private static final int COMPRESSION_LEVEL = 6;
	private static final int MAX_RETRY_ATTEMPTS = 3;

	private final CompressionMetrics metrics;
	private final CompressionStrategy strategy;
	private final ThreadLocal<Deflater> deflater;
	private final ThreadLocal<Inflater> inflater;

	public MetricsCompressionManager() {
		this.metrics = new CompressionMetrics();
		this.strategy = new CompressionStrategy();
		this.deflater = ThreadLocal.withInitial(() -> new Deflater(COMPRESSION_LEVEL));
		this.inflater = ThreadLocal.withInitial(Inflater::new);
	}

	public byte[] compress(byte[] data) throws CompressionException {
		if (data == null || data.length == 0) {
			return new byte[0];
		}

		long startTime = System.nanoTime();
		try {
			byte[] compressed = strategy.compress(data, deflater.get());
			metrics.recordCompression(data.length, compressed.length, System.nanoTime() - startTime);
			return compressed;
		} catch (Exception e) {
			metrics.recordError("compression");
			throw new CompressionException("Échec de la compression", e);
		}
	}

	public byte[] decompress(byte[] compressedData) throws CompressionException {
		if (compressedData == null || compressedData.length == 0) {
			return new byte[0];
		}

		long startTime = System.nanoTime();
		try {
			byte[] decompressed = strategy.decompress(compressedData, inflater.get());
			metrics.recordDecompression(compressedData.length, decompressed.length,
					System.nanoTime() - startTime);
			return decompressed;
		} catch (Exception e) {
			metrics.recordError("decompression");
			throw new CompressionException("Échec de la décompression", e);
		}
	}

	private class CompressionStrategy {
		byte[] compress(byte[] data, Deflater deflater) throws IOException {
			deflater.reset();
			deflater.setInput(data);
			deflater.finish();

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
				byte[] buffer = new byte[BUFFER_SIZE];
				while (!deflater.finished()) {
					int count = deflater.deflate(buffer);
					outputStream.write(buffer, 0, count);
				}
				return outputStream.toByteArray();
			}
		}

		byte[] decompress(byte[] compressedData, Inflater inflater) throws IOException {
			inflater.reset();
			inflater.setInput(compressedData);

			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length)) {
				byte[] buffer = new byte[BUFFER_SIZE];
				while (!inflater.finished()) {
					int count = inflater.inflate(buffer);
					outputStream.write(buffer, 0, count);
				}
				return outputStream.toByteArray();
			} catch (DataFormatException e) {
				throw new IOException("Format de données invalide", e);
			}
		}
	}

	private static class CompressionMetrics {
		private final AtomicLong totalCompressed = new AtomicLong(0);
		private final AtomicLong totalDecompressed = new AtomicLong(0);
		private final AtomicLong compressionTime = new AtomicLong(0);
		private final AtomicLong decompressionTime = new AtomicLong(0);
		private final AtomicInteger errorCount = new AtomicInteger(0);
		private final Map<String, AtomicInteger> errorsByType = new ConcurrentHashMap<>();

		void recordCompression(long originalSize, long compressedSize, long duration) {
			totalCompressed.addAndGet(originalSize);
			compressionTime.addAndGet(duration);
		}

		void recordDecompression(long compressedSize, long decompressedSize, long duration) {
			totalDecompressed.addAndGet(decompressedSize);
			decompressionTime.addAndGet(duration);
		}

		void recordError(String type) {
			errorCount.incrementAndGet();
			errorsByType.computeIfAbsent(type, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalCompressed", totalCompressed.get(),
					"totalDecompressed", totalDecompressed.get(),
					"averageCompressionTime", getAverageCompressionTime(),
					"averageDecompressionTime", getAverageDecompressionTime(),
					"errorCount", errorCount.get(),
					"errorsByType", new HashMap<>(errorsByType));
		}

		private double getAverageCompressionTime() {
			long total = totalCompressed.get();
			return total > 0 ? (double) compressionTime.get() / total : 0;
		}

		private double getAverageDecompressionTime() {
			long total = totalDecompressed.get();
			return total > 0 ? (double) decompressionTime.get() / total : 0;
		}
	}

	@Override
	public void close() {
		ThreadLocal<Deflater> deflaterRef = deflater;
		ThreadLocal<Inflater> inflaterRef = inflater;

		deflater = null;
		inflater = null;

		if (deflaterRef != null) {
			Deflater def = deflaterRef.get();
			if (def != null) {
				def.end();
			}
			deflaterRef.remove();
		}

		if (inflaterRef != null) {
			Inflater inf = inflaterRef.get();
			if (inf != null) {
				inf.end();
			}
			inflaterRef.remove();
		}
	}
}
