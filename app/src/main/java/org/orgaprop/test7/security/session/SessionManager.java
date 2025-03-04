package org.orgaprop.test7.security.session;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import org.orgaprop.test7.controllers.activities.MainActivity;
import org.orgaprop.test7.security.config.SecurityConfig;
import org.orgaprop.test7.services.Prefs;

public class SessionManager {

	private static volatile SessionManager instance;

	// États de session
	private boolean isFirst;
	private boolean isConnected;
	private String idMbr;
	private String adrMac;
	private boolean hasContrat;
	private JSONObject structure;
	private JSONObject mapAgences;

	// Stockage des préférences
	private final SharedPreferences preferences;
	private final Prefs prefs;

	private long lastActivityTime;
	private boolean isAuthenticated;
	private int nbLoginAttempts;

	private SessionManager(Context context) {
		this.preferences = context.getSharedPreferences(MainActivity.PREF_NAME_APPLI, Context.MODE_PRIVATE);
		this.prefs = new Prefs(context);
		this.nbLoginAttempts = 0;
		resetSession();
	}

	public static SessionManager getInstance(Context context) {
		if (instance == null) {
			synchronized (SessionManager.class) {
				if (instance == null) {
					instance = new SessionManager(context);
				}
			}
		}
		return instance;
	}

	public void updateActivity() {
		lastActivityTime = System.currentTimeMillis();
	}

	public boolean isSessionValid() {
		long currentTime = System.currentTimeMillis();
		return isAuthenticated &&
				(currentTime - lastActivityTime) < SecurityConfig.Validation.SESSION_TIMEOUT;
	}

	public void initializeSession(JSONObject jsonResponse) throws JSONException {
		if (!jsonResponse.has("idMbr") || !jsonResponse.has("adrMac")) {
			throw new IllegalArgumentException("JSON de réponse invalide: champs requis manquants");
		}

		idMbr = jsonResponse.getString("idMbr");
		adrMac = jsonResponse.getString("adrMac");
		isFirst = false;
		isConnected = true;
		hasContrat = jsonResponse.getBoolean("hasContrat");
		structure = jsonResponse.getJSONObject("structure");
		mapAgences = jsonResponse.getJSONObject("agences");
		saveSessionData(jsonResponse);
	}

	private void saveSessionData(JSONObject jsonResponse) throws JSONException {
		prefs.setMbr(idMbr);
		prefs.setAdrMac(adrMac);

		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(MainActivity.PREF_KEY_LIMIT_TOP, jsonResponse.getJSONObject("limits").getString("top"));
		editor.putString(MainActivity.PREF_KEY_LIMIT_DOWN, jsonResponse.getJSONObject("limits").getString("down"));
		editor.putString(MainActivity.PREF_KEY_PLAN_ACTION, jsonResponse.getString("planAction"));
		editor.putString(MainActivity.PREF_KEY_LIM_RAPPORT, jsonResponse.getJSONObject("rapport").getString("limite"));
		editor.putString(MainActivity.PREF_KEY_DEST_RAPPORT,
				jsonResponse.getJSONObject("rapport").getString("destinataire"));
		editor.putString(MainActivity.PREF_KEY_INFO_PROD, jsonResponse.getJSONObject("info").getString("prod"));
		editor.putString(MainActivity.PREF_KEY_INFO_AFF, jsonResponse.getJSONObject("info").getString("aff"));
		editor.putString(MainActivity.PREF_KEY_CURRENT_DATE, jsonResponse.getString("currentDate"));
		editor.apply();
	}

	public void resetSession() {
		isFirst = true;
		isConnected = false;
		idMbr = "new";
		adrMac = "new";
		hasContrat = false;
		structure = new JSONObject();
		mapAgences = new JSONObject();
		lastActivityTime = 0;
		isAuthenticated = false;

		// Nettoyage des préférences
		prefs.setMbr("new");
		prefs.setAgency("");
		prefs.setGroup("");
		prefs.setResidence("");
	}

	// Getters et setters pour les états
	public boolean isFirst() {
		return isFirst;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public String getIdMbr() {
		return idMbr;
	}

	public String getAdrMac() {
		return adrMac;
	}

	public boolean hasContrat() {
		return hasContrat;
	}

	public JSONObject getStructure() throws JSONException {
		return new JSONObject(structure.toString());
	}

	public JSONObject getMapAgences() throws JSONException {
		return new JSONObject(mapAgences.toString());
	}

	public long getLastActivityTime() { return lastActivityTime; }

	public int incrementAndGetAttempts() {
		nbLoginAttempts++;
		return nbLoginAttempts;
	}

}
