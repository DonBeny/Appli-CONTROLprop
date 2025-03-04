package org.orgaprop.test7.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.StringRes;

public class UiUtils {

	private UiUtils() {
		// Empêche l'instanciation
	}

	public static void showWait(Activity activity, ImageView waitImage, boolean show) {
		runOnMainThread(() -> waitImage.setVisibility(show ? View.VISIBLE : View.INVISIBLE));
	}

	public static void disableOption(View... views) {
		for (View view : views) {
			view.setEnabled(false);
			view.setClickable(false);
		}
	}

	public static void showToast(Context context, @StringRes int messageId) {
		runOnMainThread(() -> Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show());
	}

	public static void toggleWaitingState(View waitingView, View mainLayout, boolean show) {
		runOnMainThread(() -> {
			waitingView.setVisibility(show ? View.VISIBLE : View.GONE);
			mainLayout.setEnabled(!show);
		});
	}

	private static void runOnMainThread(Runnable action) {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			action.run();
		} else {
			new Handler(Looper.getMainLooper()).post(action);
		}
	}

}