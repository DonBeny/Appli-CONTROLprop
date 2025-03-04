package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class WorkflowManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(WorkflowManager.class);

	private static final int MAX_CONCURRENT_WORKFLOWS = 10;
	private static final long WORKFLOW_TIMEOUT = 30_000; // 30 secondes
	private static final int QUEUE_CAPACITY = 100;

	private final Map<String, WorkflowDefinition> workflows;
	private final BlockingQueue<WorkflowExecution> executionQueue;
	private final ExecutorService executor;
	private final AtomicBoolean isRunning;
	private final WorkflowMetrics metrics;
	private final AlertManager alertManager;

	public WorkflowManager(AlertManager alertManager) {
		this.workflows = new ConcurrentHashMap<>();
		this.executionQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
		this.executor = createExecutor();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new WorkflowMetrics();
		this.alertManager = alertManager;

		startWorkflowProcessor();
	}

	public void registerWorkflow(String workflowId, WorkflowDefinition definition) {
		if (!isRunning.get()) {
			throw new IllegalStateException("WorkflowManager est arrêté");
		}
		workflows.put(workflowId, definition);
		metrics.recordWorkflowRegistration(workflowId);
	}

	public void executeWorkflow(String workflowId, Map<String, Object> context) {
		WorkflowDefinition definition = workflows.get(workflowId);
		if (definition == null) {
			throw new IllegalArgumentException("Workflow non trouvé: " + workflowId);
		}

		try {
			WorkflowExecution execution = new WorkflowExecution(workflowId, definition, context);
			if (!executionQueue.offer(execution)) {
				handleQueueFull(execution);
				return;
			}
			metrics.recordWorkflowSubmission(workflowId);
		} catch (Exception e) {
			handleExecutionError(e, workflowId);
		}
	}

	private void startWorkflowProcessor() {
		for (int i = 0; i < MAX_CONCURRENT_WORKFLOWS; i++) {
			executor.submit(this::processWorkflows);
		}
	}

	private void processWorkflows() {
		while (isRunning.get()) {
			try {
				WorkflowExecution execution = executionQueue.poll(1, TimeUnit.SECONDS);
				if (execution != null) {
					processWorkflow(execution);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void processWorkflow(WorkflowExecution execution) {
		long startTime = System.nanoTime();
		try {
			execution.execute();
			metrics.recordWorkflowCompletion(execution.workflowId, System.nanoTime() - startTime);
		} catch (Exception e) {
			handleWorkflowError(e, execution);
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
		}
	}

	private static class WorkflowExecution {
		final String workflowId;
		final WorkflowDefinition definition;
		final Map<String, Object> context;
		final long startTime;

		WorkflowExecution(String workflowId, WorkflowDefinition definition, Map<String, Object> context) {
			this.workflowId = workflowId;
			this.definition = definition;
			this.context = new ConcurrentHashMap<>(context);
			this.startTime = System.currentTimeMillis();
		}

		void execute() {
			definition.execute(context);
		}
	}

	public interface WorkflowDefinition {
		void execute(Map<String, Object> context);
	}

	private static class WorkflowMetrics {
		private final Map<String, AtomicInteger> submissionsByWorkflow = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> completionsByWorkflow = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> executionTimeByWorkflow = new ConcurrentHashMap<>();
		private final AtomicInteger totalWorkflows = new AtomicInteger(0);

		void recordWorkflowRegistration(String workflowId) {
			totalWorkflows.incrementAndGet();
		}

		void recordWorkflowSubmission(String workflowId) {
			submissionsByWorkflow.computeIfAbsent(workflowId, k -> new AtomicInteger())
					.incrementAndGet();
		}

		void recordWorkflowCompletion(String workflowId, long duration) {
			completionsByWorkflow.computeIfAbsent(workflowId, k -> new AtomicInteger())
					.incrementAndGet();
			executionTimeByWorkflow.computeIfAbsent(workflowId, k -> new AtomicLong())
					.addAndGet(duration);
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalWorkflows", totalWorkflows.get(),
					"submissions", new HashMap<>(submissionsByWorkflow),
					"completions", new HashMap<>(completionsByWorkflow),
					"averageExecutionTimes", getAverageExecutionTimes());
		}

		private Map<String, Double> getAverageExecutionTimes() {
			Map<String, Double> averages = new HashMap<>();
			completionsByWorkflow.forEach((workflow, completions) -> {
				long totalTime = executionTimeByWorkflow.getOrDefault(workflow, new AtomicLong()).get();
				averages.put(workflow, completions.get() > 0 ? (double) totalTime / completions.get() : 0);
			});
			return averages;
		}
	}
}
