package org.orgaprop.test7.managers;

import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.orgaprop.test7.exceptions.BaseException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestionnaire des permissions de l'application
 */
public class PermissionManager {
	private static final String TAG = PermissionManager.class.getSimpleName();
	private final AppCompatActivity activity;
	private ActivityResultLauncher<String[]> permissionLauncher;
	private Consumer<Boolean> permissionCallback;

	private static final String[] REQUIRED_PERMISSIONS = {
			Manifest.permission.INTERNET,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.READ_CALENDAR,
			Manifest.permission.WRITE_CALENDAR
	};

	private static final String[] MAIN_ACTIVITY_PERMISSIONS = {
			Manifest.permission.INTERNET,
			Manifest.permission.READ_CALENDAR,
			Manifest.permission.WRITE_CALENDAR
	};

	public PermissionManager(AppCompatActivity activity) {
		this.activity = activity;
		initializePermissionLauncher();
	}

	private void initializePermissionLauncher() {
		permissionLauncher = activity.registerForActivityResult(
				new ActivityResultContracts.RequestMultiplePermissions(),
				result -> {
					boolean allGranted = result.values().stream().allMatch(granted -> granted);
					if (permissionCallback != null) {
						permissionCallback.accept(allGranted);
					}
					Log.d(TAG, "Permissions " + (allGranted ? "accordées" : "refusées"));
				});
	}

	/**
	 * Interface pour les callbacks de résultat des permissions
	 */
	public interface PermissionResultCallback {
		void onResult(boolean granted) throws BaseException;

		default void onError(Exception e) {
			Log.e(TAG, "Erreur lors de la demande de permissions", e);
		}
	}

	/**
	 * Version améliorée de checkRequiredPermissions avec gestion d'erreurs
	 */
	public boolean checkRequiredPermissions(PermissionResultCallback callback) {
		try {
			List<String> permissionsToRequest = getPermissionsToRequest(REQUIRED_PERMISSIONS);

			if (permissionsToRequest.isEmpty()) {
				callback.onResult(true);
				return true;
			}

			requestPermissions(permissionsToRequest, callback);
			return false;
		} catch (Exception e) {
			callback.onError(e);
			return false;
		}
	}

	public boolean checkMainActivityPermissions(PermissionResultCallback callback) {
		try {
			List<String> permissionsToRequest = getPermissionsToRequest(MAIN_ACTIVITY_PERMISSIONS);

			if (permissionsToRequest.isEmpty()) {
				callback.onResult(true);
				return true;
			}

			requestPermissions(permissionsToRequest, new PermissionResultCallback() {
				@Override
				public void onResult(boolean granted) {
					if (!granted) {
						handleDeniedPermissions(permissionsToRequest);
					}
					callback.onResult(granted);
				}

				@Override
				public void onError(Exception e) {
					Log.e(TAG, "Erreur lors de la demande de permissions", e);
					callback.onError(e);
				}
			});
			return false;
		} catch (Exception e) {
			callback.onError(e);
			return false;
		}
	}

	private void handleDeniedPermissions(List<String> deniedPermissions) {
		for (String permission : deniedPermissions) {
			switch (permission) {
				case Manifest.permission.INTERNET:
					activity.finish(); // Permission Internet obligatoire
					break;
				case Manifest.permission.READ_CALENDAR:
				case Manifest.permission.WRITE_CALENDAR:
					Log.w(TAG, "Permissions calendrier refusées");
					break;
			}
		}
	}

	private List<String> getPermissionsToRequest(String[] permissions) {
		List<String> permissionsToRequest = new ArrayList<>();
		for (String permission : permissions) {
			if (!isPermissionGranted(permission)) {
				permissionsToRequest.add(permission);
			}
		}
		return permissionsToRequest;
	}

	private void requestPermissions(List<String> permissions, PermissionResultCallback callback) {
		this.permissionCallback = granted -> {
			try {
				callback.onResult(granted);
			} catch (Exception e) {
				callback.onError(e);
			}
		};
		permissionLauncher.launch(permissions.toArray(new String[0]));
	}

	/**
	 * Vérifie et demande les permissions nécessaires
	 * 
	 * @param callback Appelé avec true si toutes les permissions sont accordées
	 * @return true si toutes les permissions sont déjà accordées
	 */
	public boolean checkRequiredPermissions(Consumer<Boolean> callback) {
		this.permissionCallback = callback;
		List<String> permissionsToRequest = new ArrayList<>();

		for (String permission : REQUIRED_PERMISSIONS) {
			if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
				permissionsToRequest.add(permission);
			}
		}

		if (permissionsToRequest.isEmpty()) {
			callback.accept(true);
			return true;
		}

		permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
		return false;
	}

	/**
	 * Vérifie si une permission spécifique est accordée
	 * 
	 * @param permission La permission à vérifier
	 * @return true si la permission est accordée
	 */
	public boolean isPermissionGranted(String permission) {
		return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Groupe les permissions requises par catégorie
	 */
	public enum PermissionGroup {
		STORAGE(Manifest.permission.WRITE_EXTERNAL_STORAGE),
		CALENDAR(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
		NETWORK(Manifest.permission.INTERNET);

		private final String[] permissions;

		PermissionGroup(String... permissions) {
			this.permissions = permissions;
		}

		public String[] getPermissions() {
			return permissions;
		}
	}

	/**
	 * Vérifie si toutes les permissions d'un groupe sont accordées
	 * 
	 * @param group Le groupe de permissions à vérifier
	 * @return true si toutes les permissions du groupe sont accordées
	 */
	public boolean areGroupPermissionsGranted(PermissionGroup group) {
		for (String permission : group.getPermissions()) {
			if (!isPermissionGranted(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Demande uniquement les permissions d'un groupe spécifique
	 * 
	 * @param group    Le groupe de permissions à demander
	 * @param callback Appelé avec le résultat
	 */
	public void requestGroupPermissions(PermissionGroup group, Consumer<Boolean> callback) {
		List<String> permissionsToRequest = new ArrayList<>();
		for (String permission : group.getPermissions()) {
			if (!isPermissionGranted(permission)) {
				permissionsToRequest.add(permission);
			}
		}

		if (permissionsToRequest.isEmpty()) {
			callback.accept(true);
			return;
		}

		this.permissionCallback = callback;
		permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
	}
}
