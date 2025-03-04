package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class ProcessManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(ProcessManager.class);

	private static final int MAX_PROCESSES = 100;
	private static final long PROCESS_TIMEOUT = 30_000; // 30 secondes
	private static final long CLEANUP_INTERVAL = 60_000; // 1 minute

	private final Map<String, ProcessContext> processes;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final ProcessMetrics metrics;
	private final AlertManager alertManager;

	public ProcessManager(AlertManager alertManager) {
		this.processes = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new ProcessMetrics();
		this.alertManager = alertManager;

		startProcessMonitoring();
	}

	public void registerProcess(String processId, ProcessConfig config) {
		if (!isRunning.get()) {
			throw new IllegalStateException("ProcessManager est arrêté");
		}

		try {
			if (processes.size() >= MAX_PROCESSES) {
				handleMaxProcessesReached(processId);
				return;
			}

			ProcessContext context = new ProcessContext(processId, config);
			processes.put(processId, context);
			metrics.recordProcessRegistration(processId);
		} catch (Exception e) {
			handleRegistrationError(e, processId);
		}
	}

	public void startProcess(String processId) {
		ProcessContext context = processes.get(processId);
		if (context == null) {
			logger.warn("Process non trouvé: {}", processId);
			return;
		}

		try {
			context.start();
			metrics.recordProcessStart(processId);
			monitorProcess(context);
		} catch (Exception e) {
			handleStartError(e, processId);
		}
	}

	private void startProcessMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkProcesses,
				CLEANUP_INTERVAL,
				CLEANUP_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void checkProcesses() {
		try {
			processes.values().forEach(this::checkProcessState);
			cleanupInactiveProcesses();
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification des processus", e);
		}
	}

	private void checkProcessState(ProcessContext process) {
		if (process.hasTimedOut()) {
			handleProcessTimeout(process);
		}

		if (process.needsRestart()) {
			restartProcess(process);
		}

		updateProcessMetrics(process);
	}

	private void monitorProcess(ProcessContext process) {
		ProcessMonitor monitor = new ProcessMonitor(process);
		scheduler.scheduleWithFixedDelay(
				monitor,
				1000,
				1000,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				stopAllProcesses();
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
		}
	}

	private static class ProcessContext {
		private final String processId;
		private final ProcessConfig config;
		private volatile ProcessState state;
		private final AtomicLong startTime;
		private final AtomicLong lastActivityTime;
		private final AtomicInteger failureCount;

		ProcessContext(String processId, ProcessConfig config) {
			this.processId = processId;
			this.config = config;
			this.state = ProcessState.CREATED;
			this.startTime = new AtomicLong(0);
			this.lastActivityTime = new AtomicLong(0);
			this.failureCount = new AtomicInteger(0);
		}

		void start() {
			state = ProcessState.RUNNING;
			startTime.set(System.currentTimeMillis());
			updateActivityTime();
		}

		void updateActivityTime() {
			lastActivityTime.set(System.currentTimeMillis());
		}

		boolean hasTimedOut() {
			return state == ProcessState.RUNNING &&
					System.currentTimeMillis() - lastActivityTime.get() > PROCESS_TIMEOUT;
		}

		boolean needsRestart() {
			return state == ProcessState.FAILED &&
					failureCount.get() < config.maxRetries;
		}
	}

	private enum ProcessState {
		CREATED, RUNNING, FAILED, COMPLETED
	}

	private static class ProcessMetrics {
		private final Map<String, AtomicInteger> startCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> runningTime = new ConcurrentHashMap<>();
		private final AtomicInteger activeProcesses = new AtomicInteger(0);

		void recordProcessRegistration(String processId) {
			activeProcesses.incrementAndGet();
		}

		void recordProcessStart(String processId) {
			incrementCounter(startCount, processId);
		}

		void recordProcessFailure(String processId) {
			incrementCounter(failureCount, processId);
		}

		void recordProcessCompletion(String processId, long duration) {
			activeProcesses.decrementAndGet();
			runningTime.computeIfAbsent(processId, k -> new AtomicLong())
					.addAndGet(duration);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String processId) {
			counters.computeIfAbsent(processId, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"activeProcesses", activeProcesses.get(),
					"starts", new HashMap<>(startCount),
					"failures", new HashMap<>(failureCount),
					"runningTimes", new HashMap<>(runningTime));
		}
	}
}
