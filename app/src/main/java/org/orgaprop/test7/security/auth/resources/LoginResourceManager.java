package org.orgaprop.test7.security.auth.resources;

import android.util.Log;

import org.orgaprop.test7.security.auth.analyzer.SecurityAnalyzer;
import org.orgaprop.test7.security.auth.cache.LoginResponseCache;
import org.orgaprop.test7.security.network.NetworkMonitor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoginResourceManager {
	private final ScheduledExecutorService scheduledExecutor;
	private final NetworkMonitor networkMonitor;
	private final SecurityAnalyzer securityAnalyzer;
	private final LoginResponseCache responseCache;

	public LoginResourceManager(NetworkMonitor networkMonitor,
			SecurityAnalyzer securityAnalyzer,
			LoginResponseCache responseCache,
			ScheduledExecutorService scheduledExecutor) {
		this.networkMonitor = networkMonitor;
		this.securityAnalyzer = securityAnalyzer;
		this.responseCache = responseCache;
		this.scheduledExecutor = scheduledExecutor;
	}

	public void dispose() {
		try {
			securityAnalyzer.shutdown();
			networkMonitor.stopMonitoring();
			scheduledExecutor.shutdownNow();
			responseCache.clearCache();
			Log.i("LoginResourceManager", "Ressources nettoyées avec succès");
		} catch (Exception e) {
			Log.e("LoginResourceManager", "Erreur lors du nettoyage des ressources", e);
		}
	}

	public void shutdown() {
		try {
			networkMonitor.stopMonitoring();
			scheduledExecutor.shutdown();
			if (!scheduledExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
				scheduledExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduledExecutor.shutdownNow();
			Thread.currentThread().interrupt();
			Log.e("LoginResourceManager", "Interruption lors de l'arrêt", e);
		}
	}
}
