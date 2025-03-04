package org.orgaprop.test7.metrics.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class MetricsTransport implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MetricsTransport.class);

	private final BlockingQueue<TransportPacket> sendQueue;
	private final ScheduledExecutorService scheduler;
	private final Map<String, TransportEndpoint> endpoints;
	private final AtomicBoolean isRunning;
	private final TransportMetrics metrics;
	private final CompressionManager compressionManager;
	private final MetricsConfig config;
	private final ConfigModule transportConfig;

	public MetricsTransport(CompressionManager compressionManager) {
		this.config = MetricsConfig.getInstance();
		this.transportConfig = config.getModule("transport");

		if (transportConfig == null) {
			logger.error("Configuration 'transport' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		Map<String, Object> batchConfig = (Map<String, Object>) transportConfig.getProperty("batch");
		int maxQueueSize = (int) batchConfig.get("maxQueueSize");

		this.sendQueue = new LinkedBlockingQueue<>(maxQueueSize);
		this.scheduler = createScheduler();
		this.endpoints = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new TransportMetrics();
		this.compressionManager = compressionManager;

		startTransport();
	}

	public void registerEndpoint(String id, TransportEndpoint endpoint) {
		endpoints.put(id, endpoint);
		metrics.recordEndpointRegistration(id);
	}

	public void send(String endpointId, Map<String, Object> data) {
		if (!isRunning.get()) {
			throw new IllegalStateException("MetricsTransport est arrêté");
		}

		TransportPacket packet = new TransportPacket(endpointId, data);
		if (!sendQueue.offer(packet)) {
			handleQueueFull(packet);
			return;
		}
		metrics.recordSubmission(endpointId);
	}

	private void startTransport() {
		Map<String, Object> batchConfig = (Map<String, Object>) transportConfig.getProperty("batch");
		long flushInterval = (long) batchConfig.get("flushInterval");

		scheduler.scheduleAtFixedRate(
				this::processQueue,
				flushInterval,
				flushInterval,
				TimeUnit.MILLISECONDS);
	}

	private void processQueue() {
		Map<String, Object> batchConfig = (Map<String, Object>) transportConfig.getProperty("batch");
		int batchSize = (int) batchConfig.get("size");

		List<TransportPacket> batch = new ArrayList<>();
		sendQueue.drainTo(batch, batchSize);

		if (!batch.isEmpty()) {
			long startTime = System.nanoTime();
			processBatch(batch);
			metrics.recordBatchProcessing(batch.size(), System.nanoTime() - startTime);
		}
	}

	private void processBatch(List<TransportPacket> batch) {
		Map<String, List<TransportPacket>> packetsByEndpoint = batch.stream()
				.collect(groupingBy(TransportPacket::getEndpointId));

		packetsByEndpoint.forEach(this::sendToEndpoint);
	}

	private void sendToEndpoint(String endpointId, List<TransportPacket> packets) {
		TransportEndpoint endpoint = endpoints.get(endpointId);
		if (endpoint == null) {
			handleMissingEndpoint(endpointId);
			return;
		}

		Map<String, Object> retryConfig = (Map<String, Object>) transportConfig.getProperty("retry");
		int maxAttempts = (int) retryConfig.get("maxAttempts");
		long initialBackoff = (long) retryConfig.get("backoffInitial");
		double backoffMultiplier = (double) retryConfig.get("backoffMultiplier");

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				byte[] compressed = compressPackets(packets);
				endpoint.send(compressed);
				metrics.recordSuccess(endpointId, packets.size());
				return;
			} catch (Exception e) {
				if (attempt == maxAttempts) {
					handleSendError(e, endpointId, packets.size());
				} else {
					long backoff = (long) (initialBackoff * Math.pow(backoffMultiplier, attempt - 1));
					Thread.sleep(Math.min(backoff, (long) retryConfig.get("maxBackoff")));
				}
			}
		}
	}

	private byte[] compressPackets(List<TransportPacket> packets) throws Exception {
		Map<String, Object> batchData = new HashMap<>();
		batchData.put("timestamp", System.currentTimeMillis());
		batchData.put("packets", packets.stream()
				.map(TransportPacket::getData)
				.collect(toList()));

		return compressionManager.compress(serializeData(batchData));
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				processRemainingPackets();
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
		}
	}

	private static class TransportPacket {
		final String endpointId;
		final Map<String, Object> data;
		final long timestamp;

		TransportPacket(String endpointId, Map<String, Object> data) {
			this.endpointId = endpointId;
			this.data = new HashMap<>(data);
			this.timestamp = System.currentTimeMillis();
		}

		String getEndpointId() {
			return endpointId;
		}

		Map<String, Object> getData() {
			return data;
		}
	}

	private static class TransportMetrics {
		private final Map<String, AtomicInteger> submissionsByEndpoint = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> successesByEndpoint = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorsByEndpoint = new ConcurrentHashMap<>();
		private final AtomicLong totalProcessed = new AtomicLong(0);
		private final AtomicLong processingTime = new AtomicLong(0);

		void recordEndpointRegistration(String endpointId) {
			submissionsByEndpoint.putIfAbsent(endpointId, new AtomicInteger(0));
			successesByEndpoint.putIfAbsent(endpointId, new AtomicInteger(0));
			errorsByEndpoint.putIfAbsent(endpointId, new AtomicInteger(0));
		}

		void recordSubmission(String endpointId) {
			incrementCounter(submissionsByEndpoint, endpointId);
		}

		void recordSuccess(String endpointId, int count) {
			incrementCounter(successesByEndpoint, endpointId, count);
			totalProcessed.addAndGet(count);
		}

		void recordError(String endpointId) {
			incrementCounter(errorsByEndpoint, endpointId);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.incrementAndGet();
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String key, int delta) {
			counters.computeIfAbsent(key, k -> new AtomicInteger())
					.addAndGet(delta);
		}

		Map<String, Object> getStats() {
			return Map.of(
					"submissions", new HashMap<>(submissionsByEndpoint),
					"successes", new HashMap<>(successesByEndpoint),
					"errors", new HashMap<>(errorsByEndpoint),
					"totalProcessed", totalProcessed.get(),
					"averageProcessingTime", getAverageProcessingTime());
		}

		private double getAverageProcessingTime() {
			long total = totalProcessed.get();
			return total > 0 ? (double) processingTime.get() / total : 0;
		}
	}
}
