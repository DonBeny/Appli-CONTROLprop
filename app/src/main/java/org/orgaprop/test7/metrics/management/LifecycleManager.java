package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;

public class LifecycleManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(LifecycleManager.class);

	private static final long CHECK_INTERVAL = 60_000; // 1 minute
	private static final long INACTIVE_THRESHOLD = 3600_000; // 1 heure

	private final Map<String, ComponentLifecycle> components;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final LifecycleMetrics metrics;
	private final AlertManager alertManager;
	private final ScheduledExecutorService healthChecker;
	private final MetricsConfig config;
	private final ConfigModule managementConfig;

	public LifecycleManager(AlertManager alertManager) {
		this.components = new ConcurrentHashMap<>();
		this.scheduler = createScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new LifecycleMetrics();
		this.alertManager = alertManager;

		this.config = MetricsConfig.getInstance();
		this.managementConfig = config.getModule("management");

		if (managementConfig == null) {
			logger.error("Configuration 'management' non trouvée");
			throw new IllegalStateException("Configuration manquante");
		}

		this.healthChecker = Executors.newScheduledThreadPool(1);
		startHealthCheck();

		startLifecycleMonitoring();
	}

	public void registerComponent(String componentId, LifecycleHandler handler) {
		ComponentLifecycle lifecycle = new ComponentLifecycle(componentId, handler);
		components.put(componentId, lifecycle);
		metrics.recordComponentRegistration(componentId);
		logger.info("Composant enregistré: {}", componentId);
	}

	public void startComponent(String componentId) {
		ComponentLifecycle lifecycle = components.get(componentId);
		if (lifecycle != null) {
			try {
				lifecycle.start();
				metrics.recordComponentStart(componentId);
			} catch (Exception e) {
				handleStartError(componentId, e);
			}
		}
	}

	public void stopComponent(String componentId) {
		ComponentLifecycle lifecycle = components.get(componentId);
		if (lifecycle != null) {
			try {
				lifecycle.stop();
				metrics.recordComponentStop(componentId);
			} catch (Exception e) {
				handleStopError(componentId, e);
			}
		}
	}

	private void startLifecycleMonitoring() {
		scheduler.scheduleAtFixedRate(
				this::checkComponents,
				CHECK_INTERVAL,
				CHECK_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void checkComponents() {
		try {
			components.forEach((id, lifecycle) -> {
				if (lifecycle.isInactive()) {
					handleInactiveComponent(id, lifecycle);
				}
				if (lifecycle.needsRestart()) {
					restartComponent(id, lifecycle);
				}
			});
		} catch (Exception e) {
			logger.error("Erreur lors de la vérification des composants", e);
		}
	}

	private void handleInactiveComponent(String componentId, ComponentLifecycle lifecycle) {
		metrics.recordInactiveComponent(componentId);
		Map<String, Object> alertData = Map.of(
				"componentId", componentId,
				"lastActivityTime", lifecycle.getLastActivityTime(),
				"state", lifecycle.getState());
		alertManager.raiseAlert("COMPONENT_INACTIVE", alertData);
	}

	private void restartComponent(String componentId, ComponentLifecycle lifecycle) {
		try {
			lifecycle.restart();
			metrics.recordComponentRestart(componentId);
			logger.info("Composant redémarré: {}", componentId);
		} catch (Exception e) {
			handleRestartError(componentId, e);
		}
	}

	@Override
	public void close() {
		if (isRunning.compareAndSet(true, false)) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				scheduler.shutdownNow();
			}
			stopAllComponents();
		}
	}

	private void startHealthCheck() {
		Map<String, Object> lifecycleConfig = (Map<String, Object>) managementConfig.getProperty("lifecycle");
		long interval = (long) lifecycleConfig.get("healthCheckInterval");

		healthChecker.scheduleAtFixedRate(
				this::checkHealth,
				0,
				interval,
				TimeUnit.MILLISECONDS);
	}

	private static class ComponentLifecycle {
		private final String componentId;
		private final LifecycleHandler handler;
		private volatile LifecycleState state;
		private volatile long lastActivityTime;
		private final AtomicInteger restartCount;

		ComponentLifecycle(String componentId, LifecycleHandler handler) {
			this.componentId = componentId;
			this.handler = handler;
			this.state = LifecycleState.STOPPED;
			this.lastActivityTime = System.currentTimeMillis();
			this.restartCount = new AtomicInteger(0);
		}

		synchronized void start() throws Exception {
			if (state != LifecycleState.RUNNING) {
				handler.onStart();
				state = LifecycleState.RUNNING;
				updateActivityTime();
			}
		}

		synchronized void stop() throws Exception {
			if (state != LifecycleState.STOPPED) {
				handler.onStop();
				state = LifecycleState.STOPPED;
			}
		}

		synchronized void restart() throws Exception {
			stop();
			start();
			restartCount.incrementAndGet();
		}

		void updateActivityTime() {
			lastActivityTime = System.currentTimeMillis();
		}

		boolean isInactive() {
			return state == LifecycleState.RUNNING &&
					System.currentTimeMillis() - lastActivityTime > INACTIVE_THRESHOLD;
		}

		boolean needsRestart() {
			return handler.needsRestart();
		}

		LifecycleState getState() {
			return state;
		}

		long getLastActivityTime() {
			return lastActivityTime;
		}

		int getRestartCount() {
			return restartCount.get();
		}
	}

	private enum LifecycleState {
		RUNNING, STOPPED, ERROR
	}

	public interface LifecycleHandler {
		void onStart() throws Exception;

		void onStop() throws Exception;

		boolean needsRestart();
	}

	private static class LifecycleMetrics {
		private final Map<String, AtomicInteger> startCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> stopCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> restartCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> errorCount = new ConcurrentHashMap<>();
		private final AtomicInteger totalComponents = new AtomicInteger(0);

		void recordComponentRegistration(String componentId) {
			totalComponents.incrementAndGet();
		}

		void recordComponentStart(String componentId) {
			incrementCounter(startCount, componentId);
		}

		void recordComponentStop(String componentId) {
			incrementCounter(stopCount, componentId);
		}

		void recordComponentRestart(String componentId) {
			incrementCounter(restartCount, componentId);
		}

		void recordComponentError(String componentId) {
			incrementCounter(errorCount, componentId);
		}

		private void incrementCounter(Map<String, AtomicInteger> counters, String componentId) {
			counters.computeIfAbsent(componentId, k -> new AtomicInteger())
					.incrementAndGet();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"totalComponents", totalComponents.get(),
					"starts", new HashMap<>(startCount),
					"stops", new HashMap<>(stopCount),
					"restarts", new HashMap<>(restartCount),
					"errors", new HashMap<>(errorCount));
		}
	}
}
