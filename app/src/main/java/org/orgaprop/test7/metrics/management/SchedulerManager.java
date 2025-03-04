package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class SchedulerManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(SchedulerManager.class);

	private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;
	private static final long DEFAULT_TIMEOUT = 30_000; // 30 secondes
	private static final int QUEUE_CAPACITY = 1000;

	private final ScheduledExecutorService scheduler;
	private final ExecutorService taskExecutor;
	private final Map<String, TaskContext> tasks;
	private final BlockingQueue<TaskRequest<?>> immediateTaskQueue;
	private final AtomicBoolean isRunning;
	private final SchedulerMetrics metrics;
	private final AlertManager alertManager;

	public SchedulerManager(AlertManager alertManager) {
		this.scheduler = createScheduler();
		this.taskExecutor = createTaskExecutor();
		this.tasks = new ConcurrentHashMap<>();
		this.immediateTaskQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new SchedulerMetrics();
		this.alertManager = alertManager;

		startTaskProcessors();
	}

	// Méthodes pour les tâches planifiées
	public ScheduledFuture<?> scheduleTask(String taskId, Runnable task, long delay, TimeUnit unit) {
		validateTask(taskId, task);
		try {
			ScheduledFuture<?> future = scheduler.schedule(
					() -> executeTask(taskId, task, TaskType.SCHEDULED),
					delay,
					unit);
			registerTask(taskId, task, future, TaskType.SCHEDULED);
			metrics.recordScheduledTask(taskId);
			return future;
		} catch (Exception e) {
			handleSchedulingError(e, taskId);
			throw e;
		}
	}

	public ScheduledFuture<?> scheduleRecurringTask(String taskId, Runnable task,
			long initialDelay, long period, TimeUnit unit) {
		validateTask(taskId, task);
		try {
			ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
					() -> executeTask(taskId, task, TaskType.RECURRING),
					initialDelay,
					period,
					unit);
			registerTask(taskId, task, future, TaskType.RECURRING);
			metrics.recordRecurringTask(taskId);
			return future;
		} catch (Exception e) {
			handleSchedulingError(e, taskId);
			throw e;
		}
	}

	// Méthodes pour les tâches immédiates
	public <T> Future<T> submitTask(String taskId, Callable<T> task, TaskPriority priority) {
		validateTask(taskId, task);
		TaskRequest<T> request = new TaskRequest<>(taskId, task, priority);
		if (!immediateTaskQueue.offer(request)) {
			handleQueueFull(request);
			throw new RejectedExecutionException("File d'attente pleine");
		}
		metrics.recordImmediateTask(taskId, priority);
		return request.future;
	}

	private void startTaskProcessors() {
		int processors = CORE_POOL_SIZE;
		for (int i = 0; i < processors; i++) {
			taskExecutor.submit(this::processImmediateTasks);
		}
	}

	private void processImmediateTasks() {
		while (isRunning.get()) {
			try {
				TaskRequest<?> request = immediateTaskQueue.poll(1, TimeUnit.SECONDS);
				if (request != null) {
					processImmediateTask(request);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private <T> void processImmediateTask(TaskRequest<T> request) {
		TaskContext context = new TaskContext(request.taskId, TaskType.IMMEDIATE, request.priority);
		tasks.put(request.taskId, context);

		try {
			T result = request.task.call();
			request.future.complete(result);
			metrics.recordTaskCompletion(request.taskId);
		} catch (Exception e) {
			handleTaskError(e, request);
		} finally {
			tasks.remove(request.taskId);
		}
	}

	public void cancelTask(String taskId) {
		TaskContext context = tasks.remove(taskId);
		if (context != null) {
			context.cancel();
			metrics.recordTaskCancellation(taskId);
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			cancelAllTasks();
			shutdownExecutors();
		}
	}

	// Classes internes
	private static class TaskContext {
		final String taskId;
		final TaskType type;
		final TaskPriority priority;
		final long startTime;
		volatile Future<?> future;
		volatile TaskState state;

		TaskContext(String taskId, TaskType type, TaskPriority priority) {
			this.taskId = taskId;
			this.type = type;
			this.priority = priority;
			this.startTime = System.currentTimeMillis();
			this.state = TaskState.QUEUED;
		}

		void cancel() {
			if (future != null) {
				future.cancel(true);
			}
			state = TaskState.CANCELLED;
		}
	}

	private enum TaskType {
		IMMEDIATE, SCHEDULED, RECURRING
	}

	private enum TaskState {
		QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
	}

	public enum TaskPriority {
		HIGH, MEDIUM, LOW
	}

	// Autres classes utilitaires et métriques...
}
