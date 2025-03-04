package org.orgaprop.test7.security.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import org.json.JSONException;
import org.json.JSONObject;

import org.orgaprop.test7.controllers.activities.MainActivity;
import org.orgaprop.test7.exceptions.BaseException;
import org.orgaprop.test7.exceptions.config.ConfigException;
import org.orgaprop.test7.security.auth.callback.LoginCallbackManager;
import org.orgaprop.test7.security.auth.device.LoginDeviceManager;
import org.orgaprop.test7.security.auth.init.LoginInitializer;
import org.orgaprop.test7.security.auth.interfaces.LoginInterfaces;
import org.orgaprop.test7.security.auth.resources.LoginResourceManager;
import org.orgaprop.test7.security.auth.validation.LoginValidation;
import org.orgaprop.test7.security.auth.metrics.LoginMetricsManager;
import org.orgaprop.test7.security.auth.analyzer.SecurityAnalyzer;
import org.orgaprop.test7.security.auth.cache.LoginResponseCache;
import org.orgaprop.test7.security.auth.statistics.LoginStatistics;
import org.orgaprop.test7.security.auth.config.LoginConfigManager;
import org.orgaprop.test7.security.auth.report.LoginReportGenerator;
import org.orgaprop.test7.security.network.NetworkMonitor;
import org.orgaprop.test7.security.session.SessionManager;
import org.orgaprop.test7.services.Prefs;
import org.orgaprop.test7.services.HttpTask;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Gère l'authentification et la session utilisateur.
 * Cette classe est thread-safe et suit le pattern Singleton.
 */
public class LoginManager implements NetworkMonitor.NetworkStateListener, SecurityAnalyzer.SecurityEventListener {

	private static final String TAG = LoginManager.class.getSimpleName();

	private static volatile LoginManager instance;

	private final Context context;
	private final LoginMetricsManager metricsManager;
	private final NetworkMonitor networkMonitor;
	private final SecurityAnalyzer securityAnalyzer;
	private final LoginResponseCache responseCache;
	private final LoginStatistics loginStatistics;
	private final LoginConfigManager configManager;
	private final LoginReportGenerator reportGenerator;
	private final LoginDeviceManager deviceManager;
	private final LoginResourceManager resourceManager;
	private final LoginCallbackManager callbackManager;
	private final LoginValidation validation;
	private final LoginInitializer initializer;
	private final SharedPreferences preferences;
	private final Prefs prefs;
	private final SessionManager sessionManager;
	private LoginInterfaces.LoginCallback loginCallback;
	private final AuthManager authManager;

	private LoginManager(Context context) {
		validateContext(context);
		this.context = context.getApplicationContext();

		// Initialisation des préférences
		this.preferences = context.getSharedPreferences(MainActivity.PREF_NAME_APPLI, Context.MODE_PRIVATE);
		this.prefs = new Prefs(context);

		// Initialisation des managers
		this.metricsManager = new LoginMetricsManager();
		this.networkMonitor = new NetworkMonitor(context);
		this.securityAnalyzer = new SecurityAnalyzer();
		this.responseCache = new LoginResponseCache();
		this.loginStatistics = new LoginStatistics();
		this.configManager = new LoginConfigManager();
		this.reportGenerator = new LoginReportGenerator();
		this.deviceManager = new LoginDeviceManager(context);
		this.validation = new LoginValidation(context);
		this.callbackManager = new LoginCallbackManager();
		this.initializer = new LoginInitializer(context, preferences, prefs);
		this.sessionManager = SessionManager.getInstance(this.context);
		this.authManager = new AuthManager(this.context);

		// Configuration du NetworkMonitor
		this.networkMonitor.setNetworkStateListener(this);
		this.networkMonitor.startMonitoring();

		// Configuration du SecurityAnalyzer
		this.securityAnalyzer.setSecurityEventListener(this);

		// Création du ResourceManager avec les dépendances
		this.resourceManager = new LoginResourceManager(
				networkMonitor,
				securityAnalyzer,
				responseCache,
				Executors.newSingleThreadScheduledExecutor());
	}

	public static LoginManager getInstance(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Context ne peut pas être null");
		}

		if (instance == null) {
			synchronized (LoginManager.class) {
				if (instance == null) {
					instance = new LoginManager(context);
				}
			}
		}
		return instance;
	}

	/* NetworkMonitor */
	@Override
	public void onNetworkLost() {
		Log.w(TAG, "Connexion réseau perdue pendant une session");
		if (loginCallback != null) {
			loginCallback.onNetworkError(new BaseException("Connexion perdue", ConfigException.NETWORK_ERROR));
		}
	}

	@Override
	public void onNetworkAvailable() {
		Log.i(TAG, "Connexion réseau rétablie");
	}

	@Override
	public void onNetworkSpeedChanged(long speedKbps) {
		// Ajuster les timeouts en fonction de la vitesse
		if (speedKbps < 1000) {
			Log.w(TAG, "Réseau lent détecté, ajustement des timeouts");
		}
	}

	/* SecurityAnalyzer */
	@Override
	public void onSuspiciousActivity(String reason, Map<String, Object> details) {
		Log.w(TAG, "Activité suspecte: "+reason+" - "+details);
		if (loginCallback != null) {
			loginCallback.onSecurityAlert(reason, details);
		}
	}

	@Override
	public void onIpBlocked(String ip) {
		Log.w(TAG, "IP bloquée: "+ip);
	}

	@Override
	public void onIpUnblocked(String ip) {
		Log.i(TAG, "IP débloquée: "+ip);
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			shutdown();
		} finally {
			super.finalize();
		}
	}

	public void dispose() {
		resourceManager.dispose();
		callbackManager.clear();
		validation.resetAttempts();
	}

	/**
	 * Tente de connecter un utilisateur.
	 * 
	 * @param username Nom d'utilisateur
	 * @param password Mot de passe
	 * @param remember Si true, sauvegarde les credentials
	 * @return Future contenant la réponse JSON du serveur
	 */
	public CompletableFuture<JSONObject> login(String username, String password, boolean remember) {
		try {
			validation.validateLoginParameters(username, password, configManager.getVersion());
			validation.checkLoginAttempts();

			String clientIp = getClientIp();
			securityAnalyzer.trackIpAttempt(clientIp);

			String requestId = generateRequestId(username);
			Optional<JSONObject> cachedResponse = responseCache.getCachedResponse(requestId);
			if (cachedResponse.isPresent()) {
				return CompletableFuture.completedFuture(cachedResponse.get());
			}

			String stringGet = configManager.buildGetParameters();
			String stringPost = configManager.buildPostParameters(username, password);

			HttpTask task = new HttpTask(context);
			return task.executeHttpTask(
					HttpTask.HTTP_TASK_ACT_CONEX,
					HttpTask.HTTP_TASK_CBL_OK,
					stringGet,
					stringPost).thenApply(result -> {
						JSONObject response = null;
						try {
							response = new JSONObject(result);
							boolean success = response.optBoolean("status", false);
							loginStatistics.recordLogin(username, System.currentTimeMillis(), success);
							metricsManager.recordLoginAttempt(username, success, System.currentTimeMillis());
							if (success) {
								saveCredentials(username, password);
								responseCache.cacheResponse(requestId, response);
							}
						} catch (JSONException | BaseException e) {
							notifyError(ConfigException.ERR_MESS_BAD_DATA, e);
						}
						return response;
					});
		} catch (Exception e) {
			notifyError("Erreur lors de la connexion", e);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				return CompletableFuture.failedFuture(e);
			} else {
				return CompletableFuture.completedFuture(null);
			}
		}
	}

	public CompletableFuture<JSONObject> logout(String username, String password) {
		try {
			checkNetworkConnection();
			validateLoginParameters(username, password);
			String stringGet = configManager.buildGetParameters();
			String stringPost = configManager.buildPostParameters(username, password);

			HttpTask task = new HttpTask(context);
			configureHttpTask(task);
			return task.executeHttpTask(
					HttpTask.HTTP_TASK_ACT_CONEX,
					HttpTask.HTTP_TASK_CBL_NO,
					stringGet,
					stringPost).thenApply(result -> {
						JSONObject response = null;
						try {
							response = new JSONObject(result);
						} catch (JSONException e) {
							notifyError(ConfigException.ERR_MESS_BAD_DATA, e);
						}
						return response;
					});
		} catch (Exception e) {
			Log.e(TAG, "Erreur lors de la déconnexion", e);
			CompletableFuture<JSONObject> future = new CompletableFuture<>();
			future.completeExceptionally(e);
			return future;
		}
	}

	public void setDeviceInfo(String version, String phoneName, String phoneModel, String phoneBuild) {
		configManager.setDeviceInfo(version, phoneName, phoneModel, phoneBuild);
	}

	/**
	 * Sauvegarde les credentials de l'utilisateur de manière sécurisée.
	 *
	 * @param username Nom d'utilisateur
	 * @param password Mot de passe en clair
	 */
	private void saveCredentials(String username, String password) throws BaseException {
        authManager.saveCredentials(username, password);
        Log.d(TAG, "Credentials sauvegardés avec succès");
    }

	/**
	 * Vérifie la version de l'application avec le serveur.
	 * 
	 * @param idMbr    ID du membre
	 * @param deviceId Identifiant unique du device
	 * @return Future contenant la réponse JSON du serveur
	 */
	public CompletableFuture<JSONObject> checkVersion(String idMbr, String deviceId) {
		try {
			String stringGet = "version=" + configManager.getVersion();
			String stringPost = String.format("mbr=%s&mac=%s", idMbr, deviceId);

			HttpTask task = new HttpTask(context);
			configureHttpTask(task);
			return task.executeHttpTask(
					HttpTask.HTTP_TASK_ACT_CONEX,
					HttpTask.HTTP_TASK_CBL_TEST,
					stringGet,
					stringPost).thenApply(result -> {
						JSONObject response = null;
						try {
							response = new JSONObject(result);
						} catch (JSONException e) {
							notifyError(ConfigException.ERR_MESS_BAD_DATA, e);
						}
						return response;
            		});

		} catch (Exception e) {
			Log.e(TAG, "Erreur lors de la vérification de version", e);
			CompletableFuture<JSONObject> future = new CompletableFuture<>();
			future.completeExceptionally(e);
			return future;
		}
	}

	/**
	 * Nettoie les données de connexion.
	 */
	public void clearLoginData() {
		prefs.setMbr(configManager.getDefaultMbrValue());
		prefs.setAgency("");
		prefs.setGroup("");
		prefs.setResidence("");

		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(MainActivity.PREF_KEY_MBR);
		editor.remove(MainActivity.PREF_KEY_PWD);
		editor.apply();
	}

	private void validateLoginParameters(String username, String password) {
		validation.validateLoginParameters(username, password, configManager.getVersion());
	}

	private void configureHttpTask(HttpTask task) {
		configureTimeouts(task);
	}

	private void configureTimeouts(HttpTask task) {
		boolean isSlowNetwork = networkMonitor.checkNetworkSpeed() < 1000; // 1Mbps
		if (isSlowNetwork) {
			Log.w(TAG, "Votre réseau est dégradé");
		}
	}

	public void setLoginCallback(LoginInterfaces.LoginCallback callback) {
		this.loginCallback = callback;
	}

	private void checkNetworkConnection() throws BaseException {
		if (!networkMonitor.checkConnectivity()) {
			String message = "Aucune connexion réseau disponible";
			Log.e(TAG, message);
			notifyError(message, new BaseException(message, ConfigException.AUTH_ERROR));
			throw new BaseException(message, ConfigException.AUTH_ERROR);
		}
	}

	private void notifyError(String message, Exception e) {
		callbackManager.notifyLoginFailure(message);
		if (e instanceof BaseException) {
			callbackManager.notifyNetworkError(e);
		}
	}

	public void resetManager() {
		sessionManager.resetSession();
		clearLoginData();
	}

	public void shutdown() {
		try {
			networkMonitor.stopMonitoring();
			resetManager();
		} finally {
			loginCallback = null;
		}
	}

	// Ajouter un addOnDestroyListener dans le constructeur
	public void addOnDestroyListener(AppCompatActivity activity) {
		activity.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_DESTROY) {
                shutdown();
                dispose();
            }
        });
	}

	private String generateRequestId(String username) {
		return String.format("%s_%d", username, System.currentTimeMillis());
	}

	/**
	 * Récupère l'adresse IP du client
	 * 
	 * @return L'adresse IP du client ou "127.0.0.1" si non disponible
	 */
	private String getClientIp() {
		try {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			Network activeNetwork = cm.getActiveNetwork();
			if (activeNetwork == null) {
				return "127.0.0.1";
			}

			NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
			if (capabilities == null) {
				return "127.0.0.1";
			}

			// Vérifie le type de connexion
			if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
				// Pour le WiFi
				WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
				return String.format("%d.%d.%d.%d",
						(ipAddress & 0xff),
						(ipAddress >> 8 & 0xff),
						(ipAddress >> 16 & 0xff),
						(ipAddress >> 24 & 0xff));
			} else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
				// Pour la connexion mobile
				try {
					for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
						NetworkInterface intf = en.nextElement();
						for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
							InetAddress inetAddress = enumIpAddr.nextElement();
							if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
								return inetAddress.getHostAddress();
							}
						}
					}
				} catch (SocketException e) {
					Log.e(TAG, "Erreur lors de la récupération de l'IP mobile", e);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Erreur lors de la récupération de l'IP", e);
		}

		return "127.0.0.1";
	}

	private void validateContext(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Context ne peut pas être null");
		}
		if (context.getApplicationContext() == null) {
			throw new IllegalStateException("ApplicationContext non disponible");
		}
	}

}
