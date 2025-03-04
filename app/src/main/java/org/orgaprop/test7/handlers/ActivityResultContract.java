package org.orgaprop.test7.handlers;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;

/**
 * Contrat pour gérer les résultats d'activité
 */
public class ActivityResultContract {

	private final ActivityResultLauncher<Intent> launcher;
	private final ActivityResultHandler resultHandler;

	public ActivityResultContract(ActivityResultHandler resultHandler) {
		this.resultHandler = resultHandler;
		this.launcher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> resultHandler.handleResult(result.getResultCode(), result.getData()));
	}

	public void launch(Intent intent) {
		launcher.launch(intent);
	}

}
