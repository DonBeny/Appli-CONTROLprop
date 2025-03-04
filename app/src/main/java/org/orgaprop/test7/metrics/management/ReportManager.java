package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ReportManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ReportManager.class);

	private static final int MAX_CONCURRENT_REPORTS = 5;
	private static final int QUEUE_CAPACITY = 100;
	private static final long REPORT_TIMEOUT = 60_000; // 1 minute

	private final Map<String, ReportTemplate> templates;
	private final BlockingQueue<ReportRequest> reportQueue;
	private final ExecutorService executor;
	private final AtomicBoolean isRunning;
	private final ReportMetrics metrics;
	private final AlertManager alertManager;
	private final ReportStorage storage;

	public ReportManager(AlertManager alertManager) {
		this.templates = new ConcurrentHashMap<>();
		this.reportQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
		this.executor = createExecutor();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ReportMetrics();
		this.alertManager = alertManager;
		this.storage = new ReportStorage();

		startReportProcessor();
	}

	public void registerTemplate(String templateId, ReportTemplate template) {
		templates.put(templateId, template);
		metrics.recordTemplateRegistration(templateId);
	}

	public Future<ReportResult> generateReport(String templateId, Map<String, Object> parameters) {
		if (!isRunning.get()) {
			throw new IllegalStateException("ReportManager est arrêté");
		}

		ReportTemplate template = templates.get(templateId);
		if (template == null) {
			throw new IllegalArgumentException("Template non trouvé: " + templateId);
		}

		CompletableFuture<ReportResult> future = new CompletableFuture<>();
		ReportRequest request = new ReportRequest(templateId, parameters, future);

		if (!reportQueue.offer(request)) {
			handleQueueFull(request);
			future.completeExceptionally(new RejectedExecutionException("File d'attente pleine"));
			return future;
		}

		metrics.recordReportRequest(templateId);
		return future;
	}

	private void startReportProcessor() {
		for (int i = 0; i < MAX_CONCURRENT_REPORTS; i++) {
			executor.submit(this::processReports);
		}
	}

	private void processReports() {
		while (isRunning.get()) {
			try {
				ReportRequest request = reportQueue.poll(1, TimeUnit.SECONDS);
				if (request != null) {
					processReport(request);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void processReport(ReportRequest request) {
		long startTime = System.nanoTime();
		try {
			ReportTemplate template = templates.get(request.templateId);
			ReportResult result = template.generate(request.parameters);
			storage.saveReport(request.templateId, result);
			metrics.recordReportGeneration(request.templateId, System.nanoTime() - startTime);
			request.future.complete(result);
		} catch (Exception e) {
			handleReportError(request, e);
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				executor.shutdownNow();
			}
			storage.close();
		}
	}

	private static class ReportRequest {
		final String templateId;
		final Map<String, Object> parameters;
		final CompletableFuture<ReportResult> future;

		ReportRequest(String templateId, Map<String, Object> parameters,
				CompletableFuture<ReportResult> future) {
			this.templateId = templateId;
			this.parameters = new HashMap<>(parameters);
			this.future = future;
		}
	}

	public interface ReportTemplate {
		ReportResult generate(Map<String, Object> parameters);
	}

	private static class ReportMetrics {
		private final Map<String, AtomicInteger> requestsByTemplate = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> generationsByTemplate = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> generationTimeByTemplate = new ConcurrentHashMap<>();
		private final AtomicInteger totalTemplates = new AtomicInteger(0);

		void recordTemplateRegistration(String templateId) {
			totalTemplates.incrementAndGet();
		}

		void recordReportRequest(String templateId) {
			incrementCounter(requestsByTemplate, templateId);
		}

		void recordReportGeneration(String templateId, long duration) {
			incrementCounter(generationsByTemplate, templateId);
			generationTimeByTemplate.computeIfAbsent(templateId, k -> new AtomicLong())
					.addAndGet(duration);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String templateId) {
			counters.computeIfAbsent(templateId, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalTemplates", totalTemplates.get(),
					"requests", new HashMap<>(requestsByTemplate),
					"generations", new HashMap<>(generationsByTemplate),
					"averageGenerationTimes", calculateAverageGenerationTimes());
		}

		private Map<String, Double> calculateAverageGenerationTimes() {
			Map<String, Double> averages = new HashMap<>();
			generationsByTemplate.forEach((template, generations) -> {
				long totalTime = generationTimeByTemplate.getOrDefault(template, new AtomicLong()).get();
				averages.put(template, generations.get() > 0 ? (double) totalTime / generations.get() : 0);
			});
			return averages;
		}
	}
}
