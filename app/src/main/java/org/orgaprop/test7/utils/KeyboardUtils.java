package org.orgaprop.test7.utils;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class KeyboardUtils {

	public static void hideKeyboard(Context context, TextView textView) {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

		if (imm != null) {
			imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
		}
	}

	public static void showKeyboard(Context context) {
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

		if (imm != null) {
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
		}
	}

}
