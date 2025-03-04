package org.orgaprop.test7.managers;

import android.content.Context;
import android.widget.Toast;

import org.orgaprop.test7.R;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.services.Prefs;

public class DisconnectionManager {

	private final Context context;
	private final Prefs prefs;
	private final Runnable onDisconnectComplete;

	public DisconnectionManager(Context context, Prefs prefs, Runnable onDisconnectComplete) {
		this.context = context;
		this.prefs = prefs;
		this.onDisconnectComplete = onDisconnectComplete;
	}

	public void disconnect(String memberId) {
		HttpTask task = new HttpTask(context);

		task.executeHttpTask(HttpTask.HTTP_TASK_ACT_CONEX, HttpTask.HTTP_TASK_CBL_NO, "",
				"mbr=" + memberId)
				.thenAccept(this::handleResult)
				.exceptionally(this::handleError);
	}

	private void handleResult(String result) {
		if (result.startsWith("1")) {
			resetPreferences();
		} else {
			Toast.makeText(context, result.substring(1), Toast.LENGTH_SHORT).show();
		}

		onDisconnectComplete.run();
	}

	private Void handleError(Throwable ex) {
		Toast.makeText(context, context.getString(R.string.mess_timeout),
				Toast.LENGTH_SHORT).show();

		onDisconnectComplete.run();

		return null;
	}

	private void resetPreferences() {
		prefs.setMbr("new");
		prefs.setAgency("");
		prefs.setGroup("");
		prefs.setResidence("");
	}

}
