package org.orgaprop.test7.managers;

import android.app.Activity;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import java.util.function.Consumer;

/**
 * Gestionnaire des mises à jour de l'application via le Play Store
 */
public class UpdateManager {
	private static final String TAG = UpdateManager.class.getSimpleName();
	private static final int UPDATE_REQUEST_CODE = 1000;
	private static final int UPDATE_STALENESS_DAYS = 3; // Nombre de jours avant de forcer la mise à jour
	private static final int UPDATE_PRIORITY_THRESHOLD = 3; // Seuil de priorité pour les mises à jour

	private final AppCompatActivity activity;
	private final AppUpdateManager appUpdateManager;
	private ActivityResultLauncher<IntentSenderRequest> updateLauncher;
	private boolean isUpdateInProgress;
	private Consumer<Boolean> updateCompleteCallback;

	public UpdateManager(AppCompatActivity activity) {
		this.activity = activity;
		this.appUpdateManager = AppUpdateManagerFactory.create(activity);
		this.isUpdateInProgress = false;
	}

	/**
	 * Initialise le launcher pour gérer le résultat de la demande de mise à jour
	 * 
	 * @param resultCallback Callback appelé après la tentative de mise à jour
	 */
	public void initializeLauncher(Consumer<Boolean> resultCallback) {
		updateLauncher = activity.registerForActivityResult(
				new ActivityResultContracts.StartIntentSenderForResult(),
				result -> {
					isUpdateInProgress = false;
					resultCallback.accept(result.getResultCode() == Activity.RESULT_OK);
				});
	}

	/**
	 * Vérifie si une mise à jour est disponible et nécessaire
	 * 
	 * @param onNoUpdate Appelé si aucune mise à jour n'est nécessaire
	 * @param onError    Appelé en cas d'erreur
	 */
	public void checkForUpdates(Runnable onNoUpdate, Consumer<Exception> onError) {
		if (isUpdateInProgress) {
			Log.d(TAG, "Une mise à jour est déjà en cours");
			return;
		}

		try {
			appUpdateManager
					.getAppUpdateInfo()
					.addOnSuccessListener(appUpdateInfo -> handleUpdateInfo(appUpdateInfo, onNoUpdate, onError))
					.addOnFailureListener(e -> handleError(e, onError));
		} catch (Exception e) {
			handleError(e, onError);
		}
	}

	private void handleUpdateInfo(AppUpdateInfo updateInfo, Runnable onNoUpdate, Consumer<Exception> onError) {
		if (isUpdateRequired(updateInfo)) {
			isUpdateInProgress = true;
			startUpdate(updateInfo, onError);
		} else {
			onNoUpdate.run();
		}
	}

	private boolean isUpdateRequired(AppUpdateInfo updateInfo) {
		return updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
				&& (updateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
						|| updateInfo.clientVersionStalenessDays() != null
								&& updateInfo.clientVersionStalenessDays() >= UPDATE_STALENESS_DAYS
						|| updateInfo.updatePriority() >= UPDATE_PRIORITY_THRESHOLD);
	}

	private void handleError(Exception e, Consumer<Exception> onError) {
		Log.e(TAG, "Erreur lors de la gestion des mises à jour", e);
		isUpdateInProgress = false;
		if (onError != null) {
			onError.accept(e);
		}
	}

	private void startUpdate(AppUpdateInfo updateInfo, Consumer<Exception> onError) {
		if (updateLauncher == null) {
			Log.e(TAG, "UpdateLauncher non initialisé");
			return;
		}

        appUpdateManager.startUpdateFlowForResult(updateInfo, updateLauncher,AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
    }

	/**
	 * Définit un callback à appeler une fois la mise à jour terminée
	 * 
	 * @param callback Le callback à appeler
	 */
	public void setUpdateCompleteCallback(Consumer<Boolean> callback) {
		this.updateCompleteCallback = callback;
	}

	/**
	 * Vérifie et reprend une mise à jour en cours si nécessaire
	 */
	public void resumeUpdate() {
		if (isUpdateInProgress) {
			appUpdateManager
					.getAppUpdateInfo()
					.addOnSuccessListener(this::handleResumeUpdate)
					.addOnFailureListener(e -> Log.e(TAG, "Erreur lors de la reprise de la mise à jour", e));
		}
	}

	private void handleResumeUpdate(AppUpdateInfo appUpdateInfo) {
		if (appUpdateInfo == null) {
			isUpdateInProgress = false;
			return;
		}

		switch (appUpdateInfo.installStatus()) {
			case InstallStatus.DOWNLOADED:
				appUpdateManager.completeUpdate();
				break;
			case InstallStatus.FAILED:
			case InstallStatus.CANCELED:
				isUpdateInProgress = false;
				if (updateCompleteCallback != null) {
					updateCompleteCallback.accept(false);
				}
				break;
			case InstallStatus.INSTALLED:
				isUpdateInProgress = false;
				if (updateCompleteCallback != null) {
					updateCompleteCallback.accept(true);
				}
				break;
		}
	}

	public void checkUpdateCriticality(Consumer<Boolean> callback) {
		appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
			boolean isCritical = appUpdateInfo.updatePriority() >= UPDATE_PRIORITY_THRESHOLD ||
					appUpdateInfo.clientVersionStalenessDays() != null &&
							appUpdateInfo.clientVersionStalenessDays() >= UPDATE_STALENESS_DAYS;
			callback.accept(isCritical);
		}).addOnFailureListener(e -> {
			Log.e(TAG, "Erreur lors de la vérification de la criticité de la mise à jour", e);
			callback.accept(false);
		});
	}
}
