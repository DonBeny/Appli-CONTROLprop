package org.orgaprop.test7.security.auth.callback;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class LoginCallbackManager {
	private static final Logger logger = LoggerFactory.getLogger(LoginCallbackManager.class);
	private LoginCallback loginCallback;
	private ConnectionState connectionCallback;
	private LoginReportCallback reportCallback;

	public void setLoginCallback(LoginCallback callback) {
		this.loginCallback = callback;
	}

	public void notifyLoginSuccess(JSONObject response) {
		if (loginCallback != null) {
			loginCallback.onLoginSuccess(response);
		}
	}

	public void notifyLoginFailure(String message) {
		if (loginCallback != null) {
			loginCallback.onLoginFailure(message);
		}
	}

	public void notifyNetworkError(Exception e) {
		if (loginCallback != null) {
			loginCallback.onNetworkError(e);
		}
	}

	public void notifySecurityAlert(String reason, Map<String, Object> details) {
		if (loginCallback != null) {
			loginCallback.onSecurityAlert(reason, details);
		}
		logger.warn("Alerte de sécurité: {} - {}", reason, details);
	}

	public void clear() {
		loginCallback = null;
		connectionCallback = null;
		reportCallback = null;
	}
}
