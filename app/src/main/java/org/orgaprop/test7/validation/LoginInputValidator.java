package org.orgaprop.test7.validation;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.Toast;
import org.orgaprop.test7.R;

public class LoginInputValidator {
	private final Context context;
	private ValidationCallback callback;

	public interface ValidationCallback {
		void onValidationError(String message);

		void onValidationSuccess();
	}

	public LoginInputValidator(Context context) {
		this.context = context;
	}

	public void setValidationCallback(ValidationCallback callback) {
		this.callback = callback;
	}

	public boolean validateLoginInputs(String username, String password, boolean rgpdAccepted) {
		// Validation du nom d'utilisateur
		if (username == null || username.trim().isEmpty()) {
			notifyError(context.getString(R.string.err_username_empty));
			return false;
		}

		// Validation du mot de passe
		if (password == null || password.trim().isEmpty()) {
			notifyError(context.getString(R.string.err_password_empty));
			return false;
		}

		// Validation de la longueur minimale
		if (username.length() < 3) {
			notifyError(context.getString(R.string.err_username_too_short));
			return false;
		}

		if (password.length() < 6) {
			notifyError(context.getString(R.string.err_password_too_short));
			return false;
		}

		// Validation RGPD
		if (!rgpdAccepted) {
			notifyError(context.getString(R.string.err_rgpd));
			return false;
		}

		if (callback != null) {
			callback.onValidationSuccess();
		}
		return true;
	}

	private void notifyError(String message) {
		if (callback != null) {
			callback.onValidationError(message);
		}
	}
}
