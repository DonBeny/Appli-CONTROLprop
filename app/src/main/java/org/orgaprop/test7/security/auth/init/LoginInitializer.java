package org.orgaprop.test7.security.auth.init;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import org.orgaprop.test7.services.Prefs;
import org.orgaprop.test7.controllers.activities.MainActivity;

public class LoginInitializer {
	private final Context context;
	private final SharedPreferences preferences;
	private final Prefs prefs;

	public LoginInitializer(Context context, SharedPreferences preferences, Prefs prefs) {
		this.context = context;
		this.preferences = preferences;
		this.prefs = prefs;
	}

	public boolean initializeApplication(JSONObject jsonResponse, String userName, String password) {
		try {
			String idMbr = jsonResponse.getString("idMbr");
			String adrMac = jsonResponse.getString("adrMac");

			prefs.setMbr(idMbr);
			prefs.setAdrMac(adrMac);

			savePreferences(jsonResponse, userName, password);
			return true;
		} catch (Exception e) {
			Log.e("LoginInitializer", "Erreur lors de l'initialisation de l'application", e);
			return false;
		}
	}

	private void savePreferences(JSONObject jsonResponse, String userName, String password) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(MainActivity.PREF_KEY_MBR, userName);
		editor.putString(MainActivity.PREF_KEY_PWD, password);
		// ... autres préférences
		editor.apply();
	}

	public void clearLoginData(String defaultMbrValue) {
		prefs.setMbr(defaultMbrValue);
		prefs.setAgency("");
		prefs.setGroup("");
		prefs.setResidence("");

		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(MainActivity.PREF_KEY_MBR);
		editor.remove(MainActivity.PREF_KEY_PWD);
		editor.apply();
	}
}
