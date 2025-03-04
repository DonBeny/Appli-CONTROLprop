package org.orgaprop.test7.metrics.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Map;
import java.util.LinkedHashMap;
import java.time.Duration;

public class CacheManager implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

	private static final long CACHE_CLEANUP_INTERVAL = 300_000; // 5 minutes
	private static final long DEFAULT_TTL = 3600_000; // 1 heure
	private static final int MAX_CACHE_SIZE = 10_000;

	private final Map<String, Cache<?>> caches;
	private final ConcurrentNavigableMap<Long, CacheState> stateHistory;
	private final ScheduledExecutorService scheduler;
	private final AtomicBoolean isRunning;
	private final CacheMetrics metrics;
	private final AlertManager alertManager;

	public CacheManager(AlertManager alertManager) {
		this.caches = new ConcurrentHashMap<>();
		this.stateHistory = new ConcurrentSkipListMap<>();
		this.scheduler = createCleanupScheduler();
		this.isRunning = new AtomicBoolean(true);
		this.metrics = new CacheMetrics();
		this.alertManager = alertManager;

		scheduleCleanup();
	}

	private ScheduledExecutorService createCleanupScheduler() {
		return Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "cache-cleanup");
			t.setDaemon(true);
			return t;
		});
	}

	public void put(String cacheId, String key, Object value) {
		Cache<?> cache = getCache(cacheId);
		cache.put(key, value);
		metrics.recordPut(cacheId);
	}

	public Object get(String cacheId, String key) {
		Cache<?> cache = getCache(cacheId);
		return cache.get(key);
	}

	private void scheduleCleanup() {
		scheduler.scheduleAtFixedRate(
				this::cleanup,
				CACHE_CLEANUP_INTERVAL,
				CACHE_CLEANUP_INTERVAL,
				TimeUnit.MILLISECONDS);
	}

	private void cleanup() {
		try {
			long startTime = System.nanoTime();
			for (Cache<?> cache : caches.values()) {
				cache.cleanup();
			}
			metrics.recordCleanup(System.nanoTime() - startTime);
		} catch (Exception e) {
			handleError(e, "cleanup");
		}
	}

	public void saveState(String cacheId) {
		Cache<?> cache = getCache(cacheId);
		CacheState state = new CacheState(cacheId, cache.getSize(), cache.getStats());
		stateHistory.put(System.currentTimeMillis(), state);
		metrics.recordStateSaved(cacheId);
	}

	public CacheState getLastState(String cacheId) {
		return stateHistory.entrySet().stream()
				.filter(e -> e.getValue().getCacheId().equals(cacheId))
				.reduce((first, second) -> second)
				.map(Map.Entry::getValue)
				.orElse(null);
	}

	@Override
	public void close() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			scheduler.shutdownNow();
		}
		caches.clear();
	}

	private Cache<?> getCache(String cacheId) {
		return caches.computeIfAbsent(cacheId, id -> new Cache<>());
	}

	private void handleError(Exception e, String methodName) {
		logger.error("Error in method {}: {}", methodName, e.getMessage(), e);
		alertManager.raiseAlert("CACHE_ERROR", Map.of("methodName", methodName, "error", e.getMessage()));
	}

	private static class CacheState {
		private final String cacheId;
		private final long size;
		private final Map<String, Object> stats;
		private final long timestamp;

		CacheState(String cacheId, long size, Map<String, Object> stats) {
			this.cacheId = cacheId;
			this.size = size;
			this.stats = new HashMap<>(stats);
			this.timestamp = System.currentTimeMillis();
		}

		String getCacheId() {
			return cacheId;
		}

		long getSize() {
			return size;
		}

		Map<String, Object> getStats() {
			return new HashMap<>(stats);
		}

		long getTimestamp() {
			return timestamp;
		}
	}

	private static class Cache<T> {
		private final Map<String, CacheEntry<T>> entries = new ConcurrentHashMap<>();
		private final AtomicLong size = new AtomicLong(0);
		private final AtomicLong hitCount = new AtomicLong(0);
		private final AtomicLong missCount = new AtomicLong(0);
		private final AtomicLong evictionCount = new AtomicLong(0);
		private final AtomicLong lastAccessTime = new AtomicLong(0);

		void put(String key, T value) {
			CacheEntry<T> entry = new CacheEntry<>(value, System.currentTimeMillis());
			entries.put(key, entry);
			size.incrementAndGet();
		}

		T get(String key) {
			lastAccessTime.set(System.currentTimeMillis());
			CacheEntry<T> entry = entries.get(key);
			if (entry != null && !entry.isExpired(DEFAULT_TTL)) {
				hitCount.incrementAndGet();
				return entry.value;
			}
			missCount.incrementAndGet();
			entries.remove(key);
			return null;
		}

		void cleanup() {
			long now = System.currentTimeMillis();
			long removed = entries.entrySet().stream()
					.filter(e -> e.getValue().isExpired(DEFAULT_TTL))
					.map(e -> {
						entries.remove(e.getKey());
						return 1L;
					})
					.count();
			evictionCount.addAndGet(removed);
			size.set(entries.size());
		}

		long getSize() {
			return size.get();
		}

		Map<String, Object> getStats() {
			return Map.of(
					"size", size.get(),
					"hitCount", hitCount.get(),
					"missCount", missCount.get(),
					"evictionCount", evictionCount.get(),
					"lastAccessTime", lastAccessTime.get());
		}
	}

	private static class CacheEntry<T> {
		final T value;
		final long creationTime;

		CacheEntry(T value, long creationTime) {
			this.value = value;
			this.creationTime = creationTime;
		}

		boolean isExpired(long ttl) {
			return System.currentTimeMillis() - creationTime > ttl;
		}
	}

	private static class CacheMetrics {
		private final AtomicLong totalCaches = new AtomicLong(0);
		private final Map<String, AtomicLong> hitCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> missCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicLong> putCount = new ConcurrentHashMap<>();
		private final Map<String, AtomicInteger> statesSaved = new ConcurrentHashMap<>();
		private final AtomicLong cleanupTime = new AtomicLong(0);

		void recordPut(String cacheId) {
			incrementCounter(putCount, cacheId);
		}

		void recordStateSaved(String cacheId) {
			incrementCounter(statesSaved, cacheId);
		}

		void recordCleanup(long duration) {
			cleanupTime.addAndGet(duration);
		}

		Map<String, Object> getStats() {
			Map<String, Object> stats = new HashMap<>();
			stats.putAll(Map.of(
					"totalCaches", totalCaches.get(),
					"hits", new HashMap<>(hitCount),
					"misses", new HashMap<>(missCount),
					"puts", new HashMap<>(putCount)));
			stats.put("statesSaved", new HashMap<>(statesSaved));
			stats.put("cleanupTime", cleanupTime.get());
			return stats;
		}

		private void incrementCounter(Map<String, ? extends AtomicLong> counterMap, String key) {
			counterMap.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
		}

		private void incrementCounter(Map<String, ? extends AtomicInteger> counterMap, String key) {
			counterMap.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
		}
	}
}
