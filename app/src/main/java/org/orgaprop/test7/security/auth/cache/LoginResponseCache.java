package org.orgaprop.test7.security.auth.cache;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LoginResponseCache {
	private static final Logger logger = LoggerFactory.getLogger(LoginResponseCache.class);
	private static final long DEFAULT_CACHE_DURATION = TimeUnit.MINUTES.toMillis(5);

	private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

	private static class CachedResponse {
		private final JSONObject response;
		private final long expirationTime;

		CachedResponse(JSONObject response, long expirationTime) {
			this.response = response;
			this.expirationTime = expirationTime;
		}

		boolean isValid() {
			return System.currentTimeMillis() < expirationTime;
		}

		JSONObject getResponse() {
			return response;
		}
	}

	public void cacheResponse(String requestId, JSONObject response) {
		cacheResponse(requestId, response, DEFAULT_CACHE_DURATION);
	}

	public void cacheResponse(String requestId, JSONObject response, long validityDuration) {
		cache.put(requestId, new CachedResponse(response,
				System.currentTimeMillis() + validityDuration));
		logger.debug("Réponse mise en cache pour: {}", requestId);
	}

	public Optional<JSONObject> getCachedResponse(String requestId) {
		CachedResponse cached = cache.get(requestId);
		if (cached != null) {
			if (cached.isValid()) {
				logger.debug("Cache hit pour: {}", requestId);
				return Optional.of(cached.getResponse());
			} else {
				logger.debug("Cache expiré pour: {}", requestId);
				cache.remove(requestId);
			}
		}
		return Optional.empty();
	}

	public void clearCache() {
		cache.clear();
		logger.debug("Cache nettoyé");
	}

	public void cleanupExpiredEntries() {
		cache.entrySet().removeIf(entry -> !entry.getValue().isValid());
		logger.debug("Entrées expirées nettoyées");
	}
}
