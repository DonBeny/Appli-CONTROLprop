package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.orgaprop.test7.BuildConfig;
import org.orgaprop.test7.R;
import org.orgaprop.test7.security.config.SplashScreenActivityConfig;
import org.orgaprop.test7.databinding.ActivitySplashScreenBinding;
import org.orgaprop.test7.exceptions.BaseException;
import org.orgaprop.test7.exceptions.config.ConfigException;
import org.orgaprop.test7.managers.PermissionManager;
import org.orgaprop.test7.managers.UpdateManager;

/**
 * Activity affichant l'écran de démarrage de l'application.
 * Gère les permissions, les mises à jour et la navigation vers l'activité
 * principale.
 */
public class SplashScreenActivity extends AppCompatActivity {
	private ActivitySplashScreenBinding binding;
	private boolean isStart = true;

	private final PermissionManager permissionManager = new PermissionManager(this);
	private final UpdateManager updateManager = new UpdateManager(this);

	/**
	 * Méthode appelée lors de la création de l'activité.
	 * Initialise les composants et vérifie les permissions et les mises à jour.
	 *
	 * @param savedInstanceState État précédent de l'activité, s'il y en a un.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initializeComponents();
		try {
			checkPermissionsAndUpdate();
		} catch (BaseException e) {
			showError(e.getMessage());
		}
	}

	/**
	 * Initialise les composants de l'activité.
	 */
	private void initializeComponents() {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		updateManager.initializeLauncher(this::handleUpdateResult);
		displayVersion();
	}

	/**
	 * Vérifie les permissions et lance la vérification des mises à jour.
	 *
	 * @throws BaseException Si une erreur survient lors de la vérification des
	 *                       permissions ou des mises à jour.
	 */
	private void checkPermissionsAndUpdate() throws BaseException {
		try {
			permissionManager.checkRequiredPermissions((PermissionManager.PermissionResultCallback) granted -> {
                if (granted) {
                    startUpdateCheck();
                } else {
                    showError(getString(R.string.error_permission_denied));
                }
            });
		} catch (Exception e) {
			throw new BaseException("Erreur lors de la vérification des permissions ou de la mise à jour",
					ConfigException.CONFIG_ERROR, e);
		}
	}


	/**
	 * Lance la vérification des mises à jour.
	 *
	 * @throws BaseException Si une erreur survient lors de la vérification des
	 *                       mises à jour.
	 */
	private void startUpdateCheck() throws BaseException {
		try {
			if (isFromPlayStore()) {
				updateManager.checkForUpdates(
						this::navigateToMain,
						error -> showError(getString(R.string.error_update_check)));
			} else {
				delayedNavigateToMain();
			}
		} catch (Exception e) {
			throw new BaseException("Erreur lors de la vérification des mises à jour", ConfigException.CONFIG_ERROR, e);
		}
	}

	/**
	 * Affiche la version de l'application.
	 */
	private void displayVersion() {
		binding.splashScreenActivityVersionTxt.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
	}

	/**
	 * Navigue vers l'activité principale après un délai.
	 */
	private void delayedNavigateToMain() {
		new Handler(Looper.getMainLooper()).postDelayed(
				this::navigateToMain,
				SplashScreenActivityConfig.SPLASH_SCREEN_DELAY);
	}

	/**
	 * Navigue vers l'activité principale.
	 */
	private void navigateToMain() {
		startActivity(new Intent(this, MainActivity.class));
		finish();
	}

	/**
	 * Gère le résultat de la vérification des mises à jour.
	 *
	 * @param success Indique si la mise à jour a réussi.
	 */
	private void handleUpdateResult(boolean success) {
		if (success) {
			Log.d(SplashScreenActivityConfig.TAG, "Mise à jour réussie");
			navigateToMain();
		} else {
			Log.w(SplashScreenActivityConfig.TAG, "Mise à jour échouée");
			updateManager.checkUpdateCriticality(isCritical -> {
				if (isCritical) {
					showCriticalUpdateDialog();
				} else {
					showUpdateWarning();
					navigateToMain();
				}
			});
		}
	}

	/**
	 * Affiche une boîte de dialogue pour les mises à jour critiques.
	 */
	private void showCriticalUpdateDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.update_critical_title)
				.setMessage(R.string.update_critical_message)
				.setCancelable(false)
				.setPositiveButton(R.string.retry, (dialog, which) -> {
					try {
						startUpdateCheck();
					} catch (BaseException e) {
						showError(e.getMessage());
					}
				})
				.setNegativeButton(R.string.quit, (dialog, which) -> finish())
				.show();
	}

	/**
	 * Affiche un avertissement pour les mises à jour.
	 */
	private void showUpdateWarning() {
		Toast.makeText(this, R.string.update_warning_message, Toast.LENGTH_LONG).show();
	}

	/**
	 * Vérifie si l'application a été installée depuis le Play Store.
	 *
	 * @return true si l'application a été installée depuis le Play Store, false
	 *         sinon.
	 * @throws BaseException Si une erreur survient lors de la vérification.
	 */
	private boolean isFromPlayStore() throws BaseException {
		try {
			String installer = getPackageManager().getInstallerPackageName(getPackageName());
			return "com.android.vending".equals(installer);
		} catch (Exception e) {
			throw new BaseException("Impossible de déterminer l'origine de l'installation", ConfigException.CONFIG_ERROR, e);
		}
	}

	/**
	 * Affiche un message d'erreur.
	 *
	 * @param message Le message d'erreur à afficher.
	 */
	private void showError(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
}