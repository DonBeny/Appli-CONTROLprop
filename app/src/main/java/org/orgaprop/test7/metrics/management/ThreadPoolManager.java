package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ThreadPoolManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

	private static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private static final int DEFAULT_MAX_POOL_SIZE = DEFAULT_CORE_POOL_SIZE * 2;
	private static final long KEEP_ALIVE_TIME = 60L;
	private static final int DEFAULT_QUEUE_CAPACITY = 1000;

	private final ExecutorService mainExecutor;
	private final ScheduledExecutorService monitoringExecutor;
	private final Map<String, ThreadPoolExecutor> customPools;
	private final AtomicBoolean isRunning;
	private final ThreadPoolMetrics metrics;
	private final AlertSystem alertSystem;

	public ThreadPoolManager(AlertSystem alertSystem) {
		this.mainExecutor = createMainExecutor();
		this.monitoringExecutor = createMonitoringExecutor();
		this.customPools = new ConcurrentHashMap<>();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ThreadPoolMetrics();
		this.alertSystem = alertSystem;

		startMonitoring();
	}

	private ExecutorService createMainExecutor() {
		return new ThreadPoolExecutor(
				DEFAULT_CORE_POOL_SIZE,
				DEFAULT_MAX_POOL_SIZE,
				KEEP_ALIVE_TIME,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
				new ThreadFactoryBuilder("main-pool"),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	private ScheduledExecutorService createMonitoringExecutor() {
		return Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryBuilder("pool-monitor"));
	}

	public void submitTask(String poolName, Runnable task, TaskPriority priority) {
		if (!isRunning.get()) {
			throw new IllegalStateException("ThreadPoolManager est arrêté");
		}

		ThreadPoolExecutor executor = getOrCreatePool(poolName);
		try {
			PrioritizedTask prioritizedTask = new PrioritizedTask(task, priority);
			executor.submit(prioritizedTask);
			metrics.recordTaskSubmission(poolName);
		} catch (RejectedExecutionException e) {
			handleRejectedTask(poolName, e);
		}
	}

	private ThreadPoolExecutor getOrCreatePool(String poolName) {
		return customPools.computeIfAbsent(poolName, k -> createCustomPool(poolName));
	}

	private ThreadPoolExecutor createCustomPool(String poolName) {
		return new ThreadPoolExecutor(
				DEFAULT_CORE_POOL_SIZE,
				DEFAULT_MAX_POOL_SIZE,
				KEEP_ALIVE_TIME,
				TimeUnit.SECONDS,
				new PriorityBlockingQueue<>(DEFAULT_QUEUE_CAPACITY),
				new ThreadFactoryBuilder(poolName),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	private void startMonitoring() {
		monitoringExecutor.scheduleAtFixedRate(
				this::monitorPools,
				1,
				1,
				TimeUnit.MINUTES);
	}

	private void monitorPools() {
		try {
			checkMainPool();
			checkCustomPools();
			updateMetrics();
		} catch (Exception e) {
			logger.error("Erreur lors de la surveillance des pools", e);
		}
	}

	private void checkMainPool() {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) mainExecutor;
		if (isPoolOverloaded(executor)) {
			handlePoolOverload("main", executor);
		}
	}

	private void checkCustomPools() {
		customPools.forEach((name, executor) -> {
			if (isPoolOverloaded(executor)) {
				handlePoolOverload(name, executor);
			}
		});
	}

	private boolean isPoolOverloaded(ThreadPoolExecutor executor) {
		int activeThreads = executor.getActiveCount();
		int maxThreads = executor.getMaximumPoolSize();
		int queueSize = executor.getQueue().size();

		return activeThreads >= maxThreads * 0.8 ||
				queueSize >= DEFAULT_QUEUE_CAPACITY * 0.8;
	}

	private void handlePoolOverload(String poolName, ThreadPoolExecutor executor) {
		Map<String, Object> alertData = Map.of(
				"poolName", poolName,
				"activeThreads", executor.getActiveCount(),
				"maxThreads", executor.getMaximumPoolSize(),
				"queueSize", executor.getQueue().size(),
				"completedTasks", executor.getCompletedTaskCount());

		alertSystem.raiseAlert("THREAD_POOL_OVERLOAD", alertData);
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			shutdownExecutors();
		}
	}

	private void shutdownExecutors() {
		monitoringExecutor.shutdown();
		mainExecutor.shutdown();
		customPools.values().forEach(ExecutorService::shutdown);

		try {
			if (!mainExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				mainExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			mainExecutor.shutdownNow();
		}
	}

	private static class ThreadFactoryBuilder implements ThreadFactory {
		private final String poolName;
		private final AtomicInteger threadCount = new AtomicInteger(1);

		ThreadFactoryBuilder(String poolName) {
			this.poolName = poolName;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, poolName + "-" + threadCount.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		}
	}

	private static class ThreadPoolMetrics {
		private final Map<String, AtomicLong> submissionsByPool = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> rejectionsByPool = new ConcurrentHashMap<>();
		private final AtomicLong totalTasksSubmitted = new AtomicLong(0);
		private final AtomicLong totalTasksRejected = new AtomicLong(0);

		void recordTaskSubmission(String poolName) {
			submissionsByPool.computeIfAbsent(poolName, k -> new AtomicLong())
					.incrementAndGet();
			totalTasksSubmitted.incrementAndGet();
		}

		void recordTaskRejection(String poolName) {
			rejectionsByPool.computeIfAbsent(poolName, k -> new AtomicLong())
					.incrementAndGet();
			totalTasksRejected.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalSubmitted", totalTasksSubmitted.get(),
					"totalRejected", totalTasksRejected.get(),
					"submissionsByPool", new HashMap<>(submissionsByPool),
					"rejectionsByPool", new HashMap<>(rejectionsByPool));
		}
	}

	public enum TaskPriority {
		HIGH, MEDIUM, LOW
	}
}
