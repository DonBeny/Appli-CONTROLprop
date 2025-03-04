package org.orgaprop.test7.handlers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.StringRes;
import org.orgaprop.test7.utils.UiUtils;

public class ErrorHandler {

	private static final String TAG = ErrorHandler.class.getSimpleName();
	private final Context context;
	private final UiStateManager uiStateManager;

	public ErrorHandler(Context context, UiStateManager uiStateManager) {
		this.context = context;
		this.uiStateManager = uiStateManager;
	}

	public void handleError(Throwable error, @StringRes int fallbackMessageId) {
		Log.e(TAG, "Error: ", error);
		showError(fallbackMessageId);
	}

	public void showError(@StringRes int messageId) {
		UiUtils.showToast(context, messageId);
	}

}
