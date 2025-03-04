package org.orgaprop.test7.security.auth.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import android.os.Build;
import android.util.Log;

import org.orgaprop.test7.security.auth.LoginManager;

public class LoginConfigManager {

	private static final String TAG = LoginManager.class.getSimpleName();

	// Timeouts
	private static final int CONNECTION_TIMEOUT = 30000;
	private static final int SOCKET_TIMEOUT = 30000;

	public static final int MAX_LOGIN_ATTEMPTS = 3;
	public static final long AUTO_LOGIN_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
	public static final int MAX_IP_ATTEMPTS = 5;
	public static final int SLOW_LOGIN_THRESHOLD = 5000; // ms
	public static final int DEGRADED_PERFORMANCE_THRESHOLD = 3000; // ms

	// Valeurs par défaut
	private static final String DEFAULT_MBR_VALUE = "new";
	private static final String ENCODING = StandardCharsets.UTF_8.name();

	// Informations du device
	private String version;
	private String phoneName;
	private String phoneModel;
	private String phoneBuild;
	private String deviceId;

	public LoginConfigManager() {
		this.deviceId = Build.FINGERPRINT;
	}

	public void setDeviceInfo(String version, String phoneName, String phoneModel, String phoneBuild) {
		this.version = version;
		this.phoneName = phoneName;
		this.phoneModel = phoneModel;
		this.phoneBuild = phoneBuild;
		Log.d(TAG, "Informations device mises à jour: version="+version+", model="+phoneModel);
	}

	public String buildGetParameters() {
		try {
			return String.format("version=%s&phone=%s&model=%s&build=%s",
					URLEncoder.encode(version, ENCODING),
					URLEncoder.encode(phoneName, ENCODING),
					URLEncoder.encode(phoneModel, ENCODING),
					URLEncoder.encode(phoneBuild, ENCODING));
		} catch (Exception e) {
			Log.e(TAG, "Erreur lors de la construction des paramètres GET", e);
			return "";
		}
	}

	public String buildPostParameters(String username, String password) {
		try {
			return String.format("psd=%s&pwd=%s&mac=%s",
					URLEncoder.encode(username, ENCODING),
					URLEncoder.encode(password, ENCODING),
					URLEncoder.encode(deviceId, ENCODING));
		} catch (Exception e) {
			Log.e(TAG, "Erreur lors de la construction des paramètres POST", e);
			return "";
		}
	}

	public int getConnectionTimeout() {
		return CONNECTION_TIMEOUT;
	}

	public int getSocketTimeout() {
		return SOCKET_TIMEOUT;
	}

	public int getMaxLoginAttempts() { return MAX_LOGIN_ATTEMPTS; }

	public long getAutoLoginTimeout() { return AUTO_LOGIN_TIMEOUT; }

	public int getMaxIpAttempts() { return MAX_IP_ATTEMPTS; }

	public int getSlowLoginThreshold() { return SLOW_LOGIN_THRESHOLD; }

	public int getDegradedPerformanceThreshold() { return DEGRADED_PERFORMANCE_THRESHOLD; }

	public String getDefaultMbrValue() {
		return DEFAULT_MBR_VALUE;
	}

	public String getDeviceId() {
		return deviceId;
	}

    public String getVersion() {
		return version;
    }
}
