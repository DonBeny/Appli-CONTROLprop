package org.orgaprop.test7.utils;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class KeyboardManager {

	private final Context context;
	private final InputMethodManager inputMethodManager;

	public KeyboardManager(Context context) {
		this.context = context;
		this.inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
	}

	public void hideKeyboard(TextView textView) {
		if (inputMethodManager != null) {
			inputMethodManager.hideSoftInputFromWindow(textView.getWindowToken(), 0);
		}
	}

	public void showKeyboard() {
		if (inputMethodManager != null) {
			inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		}
	}

}
