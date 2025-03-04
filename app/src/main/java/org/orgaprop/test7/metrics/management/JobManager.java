package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class JobManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

	private static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private static final int MAX_QUEUE_SIZE = 1000;
	private static final long DEFAULT_JOB_TIMEOUT = 30_000; // 30 secondes

	private final ExecutorService executor;
	private final BlockingQueue<MetricsJob> jobQueue;
	private final Map<String, JobStatus> jobStatuses;
	private final AtomicBoolean isRunning;
	private final JobMetrics metrics;
	private final JobPrioritizer prioritizer;

	public JobManager() {
		this.executor = createExecutor();
		this.jobQueue = new PriorityBlockingQueue<>(MAX_QUEUE_SIZE,
				(j1, j2) -> j2.getPriority().compareTo(j1.getPriority()));
		this.jobStatuses = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new JobMetrics();
		this.prioritizer = new JobPrioritizer();

		startJobProcessor();
	}

	public void submitJob(String jobId, Runnable task, JobPriority priority) {
		if (!isRunning.get()) {
			throw new IllegalStateException("JobManager est arrêté");
		}

		MetricsJob job = new MetricsJob(jobId, task, priority);
		if (!jobQueue.offer(job)) {
			handleQueueFull(job);
			return;
		}

		jobStatuses.put(jobId, new JobStatus(jobId, priority));
		metrics.recordJobSubmission(priority);
		logger.debug("Job soumis: {} avec priorité {}", jobId, priority);
	}

	private void startJobProcessor() {
		int threadCount = DEFAULT_THREAD_POOL_SIZE;
		for (int i = 0; i < threadCount; i++) {
			executor.submit(this::processJobs);
		}
	}

	private void processJobs() {
		while (isRunning.get()) {
			try {
				MetricsJob job = jobQueue.poll(1, TimeUnit.SECONDS);
				if (job != null) {
					processJob(job);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void processJob(MetricsJob job) {
		JobStatus status = jobStatuses.get(job.getId());
		if (status == null)
			return;

		status.setStatus(JobState.RUNNING);
		long startTime = System.nanoTime();

		try {
			job.getTask().run();
			handleJobSuccess(job, startTime);
		} catch (Exception e) {
			handleJobError(job, e);
		}
	}

	private void handleJobSuccess(MetricsJob job, long startTime) {
		long duration = System.nanoTime() - startTime;
		JobStatus status = jobStatuses.get(job.getId());
		if (status != null) {
			status.setStatus(JobState.COMPLETED);
			status.setCompletionTime(System.currentTimeMillis());
		}
		metrics.recordJobCompletion(job.getPriority(), duration);
	}

	private void handleJobError(MetricsJob job, Exception e) {
		logger.error("Erreur dans le job {}: {}", job.getId(), e.getMessage());
		JobStatus status = jobStatuses.get(job.getId());
		if (status != null) {
			status.setStatus(JobState.FAILED);
			status.setError(e.getMessage());
		}
		metrics.recordJobFailure(job.getPriority());
	}

	public Map<String, Object> getJobStats() {
		return metrics.getStats();
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
		}
	}

	private enum JobState {
		QUEUED, RUNNING, COMPLETED, FAILED
	}

	private enum JobPriority {
		HIGH, MEDIUM, LOW
	}

	private static class JobMetrics {
		private final Map<JobPriority, AtomicInteger> submissionsByPriority = new ConcurrentHashMap<>();
		private final Map<JobPriority, AtomicInteger> completionsByPriority = new ConcurrentHashMap<>();
		private final Map<JobPriority, AtomicInteger> failuresByPriority = new ConcurrentHashMap<>();
		private final Map<JobPriority, AtomicLong> totalDurationByPriority = new ConcurrentHashMap<>();

		void recordJobSubmission(JobPriority priority) {
			submissionsByPriority.computeIfAbsent(priority, k -> new AtomicInteger())
					.incrementAndGet();
		}

		void recordJobCompletion(JobPriority priority, long duration) {
			completionsByPriority.computeIfAbsent(priority, k -> new AtomicInteger())
					.incrementAndGet();
			totalDurationByPriority.computeIfAbsent(priority, k -> new AtomicLong())
					.addAndGet(duration);
		}

		void recordJobFailure(JobPriority priority) {
			failuresByPriority.computeIfAbsent(priority, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			Map<String, Object> stats = new HashMap<>();
			stats.put("submissions", getSubmissionStats());
			stats.put("completions", getCompletionStats());
			stats.put("failures", getFailureStats());
			stats.put("averageDurations", getAverageDurations());
			return stats;
		}

		private Map<JobPriority, Integer> getSubmissionStats() {
			Map<JobPriority, Integer> stats = new HashMap<>();
			submissionsByPriority.forEach((k, v) -> stats.put(k, v.get()));
			return stats;
		}

		private Map<JobPriority, Integer> getCompletionStats() {
			Map<JobPriority, Integer> stats = new HashMap<>();
			completionsByPriority.forEach((k, v) -> stats.put(k, v.get()));
			return stats;
		}

		private Map<JobPriority, Integer> getFailureStats() {
			Map<JobPriority, Integer> stats = new HashMap<>();
			failuresByPriority.forEach((k, v) -> stats.put(k, v.get()));
			return stats;
		}

		private Map<JobPriority, Double> getAverageDurations() {
			Map<JobPriority, Double> averages = new HashMap<>();
			completionsByPriority.forEach((priority, completions) -> {
				long totalDuration = totalDurationByPriority.getOrDefault(priority, new AtomicLong()).get();
				averages.put(priority, completions.get() > 0 ? (double) totalDuration / completions.get() : 0);
			});
			return averages;
		}
	}
}
