package org.orgaprop.test7.security.auth.session;

import android.util.Log;

import org.json.JSONObject;
import org.orgaprop.test7.security.auth.LoginManager;

public class UserSession {

	private static final String TAG = LoginManager.class.getSimpleName();

	private String userId;
	private String deviceId;
	private long lastActivityTime;
	private boolean isAuthenticated;

	public void initializeFromResponse(JSONObject loginResponse) {
		try {
			this.userId = loginResponse.getString("idMbr");
			this.deviceId = loginResponse.getString("adrMac");
			this.isAuthenticated = true;
			updateActivity();
			Log.i(TAG, "Session initialisée pour l'utilisateur: "+userId);
		} catch (Exception e) {
			Log.e(TAG, "Erreur lors de l'initialisation de la session", e);
			reset();
		}
	}

	public void updateActivity() {
		this.lastActivityTime = System.currentTimeMillis();
	}

	public void reset() {
		this.userId = null;
		this.deviceId = null;
		this.isAuthenticated = false;
		this.lastActivityTime = 0;
		Log.i(TAG, "Session réinitialisée");
	}

	public boolean isValid() {
		return isAuthenticated && userId != null;
	}

	// Getters
	public String getUserId() {
		return userId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public long getLastActivityTime() {
		return lastActivityTime;
	}

	public boolean isAuthenticated() {
		return isAuthenticated;
	}
}
