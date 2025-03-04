package org.orgaprop.test7.security.auth.interfaces;

import org.json.JSONObject;
import java.util.Map;

public class LoginInterfaces {

	public interface LoginCallback {

		void onLoginSuccess(JSONObject response);

		void onLoginFailure(String message);

		void onNetworkError(Exception e);

		void onSecurityAlert(String reason, Map<String, Object> details);
	}

}
