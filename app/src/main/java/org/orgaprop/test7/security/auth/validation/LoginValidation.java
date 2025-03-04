package org.orgaprop.test7.security.auth.validation;

import android.content.Context;
import android.util.Log;

import javax.security.auth.login.LoginException;

public class LoginValidation {
	private static final int MAX_LOGIN_ATTEMPTS = 3;

	private final Context context;
	private int loginAttempts;

	public LoginValidation(Context context) {
		this.context = context;
		this.loginAttempts = 0;
	}

	public void validateLoginParameters(String username, String password, String version) {
		if (username == null || username.trim().isEmpty()) {
			throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas être vide");
		}
		if (password == null || password.trim().isEmpty()) {
			throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
		}
		if (version == null || version.trim().isEmpty()) {
			throw new IllegalStateException("Les informations du device ne sont pas initialisées");
		}
	}

	public void checkLoginAttempts() throws LoginException {
		if (++loginAttempts > MAX_LOGIN_ATTEMPTS) {
			Log.w("LoginValidation", "Trop de tentatives de connexion");
			throw new LoginException("Nombre maximum de tentatives atteint");
		}
	}

	public void resetAttempts() {
		loginAttempts = 0;
	}

	public void validateContext(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Context ne peut pas être null");
		}
		if (context.getApplicationContext() == null) {
			throw new IllegalStateException("ApplicationContext non disponible");
		}
	}
}
