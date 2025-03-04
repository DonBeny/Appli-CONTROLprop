package org.orgaprop.test7.contracts;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

/**
 * Contrat de gestion des résultats d'activité
 */
public class ActivityResultContract {
	private static final String TAG = ActivityResultContract.class.getSimpleName();
	private final ActivityResultHandler resultHandler;
	private final ActivityResultLauncher<Intent> launcher;

	public ActivityResultContract(AppCompatActivity activity, ActivityResultHandler resultHandler) {
		this.resultHandler = resultHandler;
		this.launcher = activity.registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> handleResult(result.getResultCode(), result.getData()));
	}

	public void launch(Intent intent) {
		launcher.launch(intent);
	}

	private void handleResult(int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || data == null)
			return;

		try {
			String type = data.getStringExtra(IntentKeys.Selection.TYPE);
			if (type == null)
				return;

			switch (type) {
				case SelectListActivity.SELECT_LIST_TYPE_SEARCH:
					resultHandler.handleSearchResult(data);
					break;
				case SelectListActivity.SELECT_LIST_TYPE_AGC:
				case SelectListActivity.SELECT_LIST_TYPE_GRP:
				case SelectListActivity.SELECT_LIST_TYPE_RSD:
					resultHandler.handleSelectionResult(type, data);
					break;
				default:
					Log.w(TAG, "Type de résultat non géré : " + type);
			}
		} catch (Exception e) {
			resultHandler.handleError(e);
		}
	}
}
