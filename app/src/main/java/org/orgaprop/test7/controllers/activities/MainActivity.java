package org.orgaprop.test7.controllers.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.controllers.activities.ui.MainActivityUIManager;
import org.orgaprop.test7.databinding.ActivityMainBinding;
import org.orgaprop.test7.security.crypto.CryptoProviderFactory;
import org.orgaprop.test7.services.HttpTask;
import org.orgaprop.test7.services.Prefs;
import org.orgaprop.test7.utils.AndyUtils;
import org.orgaprop.test7.constants.MainActivityConstants;
import org.orgaprop.test7.security.auth.LoginManager;
import org.orgaprop.test7.security.crypto.CryptoProvider;
import org.orgaprop.test7.managers.PermissionManager;
import org.orgaprop.test7.validation.LoginInputValidator;

import java.util.Map;

public class MainActivity extends BaseActivity {

	// ********* PRIVATE VARIABLES

	private static final String TAG = "MainActivity";
	private static MainActivity instance;

	private SharedPreferences Preferences;
	private Prefs prefs;
	private boolean isFirst;
	private boolean isConnected;
	private String userName;
	private String password;
	private int version = MainActivityConstants.Config.DEFAULT_VERSION;
	private String idMbr;
	private String adrMac;
	private boolean hasContrat;
	private boolean debugg = MainActivityConstants.Config.DEBUG_MODE;
	private JSONObject mapAgences;
	private String phoneName;
	private String phoneModel;
	private String phoneBuild;
	private JSONObject structure;

	// ********* PUBLIC VARIABLES

	public static final String PREF_NAME_APPLI = "ControlProp";
	public static final String PREF_KEY_MBR = "mbr";
	public static final String PREF_KEY_PWD = "pwd";
	public static final String PREF_KEY_CURRENT_DATE = "current_date";
	public static final String PREF_KEY_LIMIT_TOP = "limit_top";
	public static final String PREF_KEY_LIMIT_DOWN = "limit_down";
	public static final String PREF_KEY_PLAN_ACTION = "plan_act";
	public static final String PREF_KEY_LIM_RAPPORT = "lim_rapport";
	public static final String PREF_KEY_DEST_RAPPORT = "dest_rapport";
	public static final String PREF_KEY_INFO_PROD = "info_prod";
	public static final String PREF_KEY_INFO_AFF = "info_aff";
	public static final String PREF_KEY_STRUCTURE_CTRL = "structure";

	public static final int UPDATE_REQUEST_CODE = 100;
	public static final int STORAGE_REQUEST_CODE = 200;

	public static boolean canAffLostConnect = true;

	public static final String ACCESS_CODE = "controlprop";

	// ********* WIDGETS

	private ActivityMainBinding binding;
	private ActivityResultLauncher<Intent> makeMainActivityLauncher;
	private LoginManager loginManager;
	private CryptoProvider cryptoProvider;
	private PermissionManager permissionManager;
	private MainActivityUIManager uiManager;
	private LoginInputValidator inputValidator;

	// ********* CONSTRUCTORS

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		binding = ActivityMainBinding.inflate(getLayoutInflater());

		setContentView(binding.getRoot());

		uiManager = new MainActivityUIManager(this, binding);

		makeMainActivityLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> handleActivityResult(result.getResultCode(), result.getData()));

		instance = this;

		Preferences = getSharedPreferences(MainActivityConstants.Preferences.PREF_NAME_APPLI, MODE_PRIVATE);
		prefs = new Prefs(this);

		phoneName = Build.BRAND;
		phoneModel = Build.DEVICE;
		phoneBuild = String.valueOf(Build.VERSION.SDK_INT);

		structure = new JSONObject();

		userName = Preferences.getString(MainActivityConstants.Preferences.KEY_MBR, "");
		try {
			cryptoProvider = CryptoProviderFactory.getInstance();
			// Récupération du mot de passe
			String encryptedPassword = Preferences.getString(MainActivityConstants.Preferences.KEY_PWD, "");
			if (!encryptedPassword.isEmpty()) {
				password = cryptoProvider.decrypt(encryptedPassword);
			}
		} catch (Exception e) {
			Log.e(TAG,"Erreur lors de l'initialisation du cryptage", e);
		}
		idMbr = MainActivityConstants.Defaults.DEFAULT_MBR_VALUE;
		adrMac = MainActivityConstants.Defaults.DEFAULT_MAC_VALUE;
		isFirst = true;
		isConnected = false;

		loginManager = LoginManager.getInstance(this);
		loginManager.setLoginCallback(this); // S'enregistrer pour les callbacks
		loginManager.setDeviceInfo(version, phoneName, phoneModel, phoneBuild);
		loginManager.addOnDestroyListener(this); // Gestion propre des ressources

		permissionManager = new PermissionManager(this);
		inputValidator = new LoginInputValidator(this);
		inputValidator.setValidationCallback(this);
		uiManager.setErrorHandler(this);
		uiManager.setUIEventListener(this);
		uiManager.setupInputListeners();

		// Ajouter des transitions personnalisées
		setupCustomTransitions();
	}

	@Override
	protected void initializeComponents() {

	}

	@Override
	protected void setupComponents() {

	}

	private void setupCustomTransitions() {
		// Transition avec glissement pour Connect -> Disconnect
		uiManager.setCustomTransition(
				MainActivityUIManager.LAYOUT_CONNECT,
				MainActivityUIManager.LAYOUT_DISCONNECT,
				MainActivityUIManager.createSlideTransition(true));

		// Transition avec fondu et échelle pour Disconnect -> Version
		uiManager.setCustomTransition(
				MainActivityUIManager.LAYOUT_DISCONNECT,
				MainActivityUIManager.LAYOUT_VERSION,
				MainActivityUIManager.createFadeScaleTransition());
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isFirst && !isConnected) {
			testIdentified();
		}

		showWait(false);
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();

		if (!isFirst) {
			if (idMbr.equals("new")) {
				try {
					uiManager.clearLoginFields();
					prefs.setMbr("new");
					openConexion();
				} catch (Exception e) {
					logger.error("Erreur lors de la réinitialisation des champs", e);
					finish(); // Fermer l'activité en cas d'erreur critique
				}
			} else {
				openDeco();
			}
		} else {
			isFirst = false;
		}
	}

	// ********* SURCHARGES

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (uiManager != null) {
			outState.putBundle(UI_STATE_KEY, uiManager.saveState());
		}
	}

	@Override
	protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Bundle uiState = savedInstanceState.getBundle(UI_STATE_KEY);
		if (uiState != null && uiManager != null) {
			uiManager.restoreState(uiState);
		}
	}

	// ********* PUBLIC FUNCTIONS

	public void mainActivityActions(View v) {
		String viewtag = v.getTag().toString();

		switch (viewtag) {
			case "on":
				showWait(true);
				connectMbr();
				break;
			case "off":
				deconectMbr();
				break;
			case "robot":
				requestConexion(HttpTask.HTTP_TASK_CBL_ROBOT);
				break;
			case "mail":
				requestConexion(HttpTask.HTTP_TASK_CBL_MAIL);
				break;
			case "rgpd":
				openWebPage();
				break;
		}
	}

	public static MainActivity getInstance() {
		return instance;
	}

	// ********* PRIVATE FUNCTIONS

	private void handleActivityResult(int resultCode, Intent data) {
		if ((resultCode == RESULT_OK) && (data != null)) {
			Intent intent = new Intent(MainActivity.this, SelectActivity.class);

			intent.putExtra(SelectActivity.SELECT_ACTIVITY_EXTRA, data.getStringExtra(SelectActivity.SELECT_ACTIVITY_EXTRA));

			startActivity(intent);
		}
	}

	private void connectMbr() {
		if (!isConnected) {
			try {
				// Utiliser validateAllFields() qui vérifie tout d'un coup
				if (uiManager.validateAllFields()) {
					String username = uiManager.getUsername();
					String password = uiManager.getPassword();

					isConnected = true;
					userName = username;
					this.password = password;

					uiManager.showWaitIndicator(true);

					loginManager.login(userName, password, uiManager.isRgpdAccepted())
							.exceptionally(this::handleLoginError);
				}
			} catch (MainActivityUIManager.UIValidationException e) {
				// La validation a échoué, afficher le message d'erreur
				runOnUiThread(() -> {
					uiManager.showToast(e.getMessage(), true);
					uiManager.showWaitIndicator(false);
				});
				isConnected = false;
			}
		}
	}

	private void deconectMbr() {
		// Déconnecte l'utilisateur
		// Nettoie les données de session

		loginManager.logout(userName, password)
				.thenAccept(jsonResponse -> runOnUiThread(() -> {
					try {
						if (jsonResponse.getBoolean("status")) {
							clearLoginData();
							finish();
						} else {
							Toast.makeText(MainActivity.this,
									jsonResponse.getString("message"),
									Toast.LENGTH_SHORT).show();
							showWait(false);
							isConnected = false;
						}
					} catch (Exception e) {
						handleLogoutError(e);
					}
				}))
				.exceptionally(ex -> {
					handleLogoutError(ex);
					return null;
				});
	}

	private void handleLoginError(Throwable e) {
		String errorMessage;
		if (e instanceof MainActivityUIManager.UIValidationException) {
			errorMessage = e.getMessage();
		} else {
			errorMessage = getString(R.string.err_conex);
		}

		runOnUiThread(() -> {
			uiManager.showToast(errorMessage, true);
			uiManager.showWaitIndicator(false);
			isConnected = false;
		});
		return null;
	}

	private void handleLogoutError(Throwable e) {
		runOnUiThread(() -> {
			Toast.makeText(MainActivity.this, "Erreur de déconnexion", Toast.LENGTH_SHORT).show();
			showWait(false);
			isConnected = false;
		});
	}

	private void clearLoginData() {
		uiManager.clearLoginFields();
		idMbr = "new";
		isFirst = true;
		isConnected = false;

		prefs.setMbr(idMbr);
		prefs.setAgency("");
		prefs.setGroup("");
		prefs.setResidence("");
	}

	private void requestConexion(String m) {
		Intent intent = new Intent(MainActivity.this, GetMailActivity.class);

		intent.putExtra(GetMailActivity.GET_MAIL_ACTIVITY_TYPE, m);

		MainActivity.this.startActivity(intent);
	}

	private void openWebPage() {
		String url = "https://www.orgaprop.org/ress/protectDonneesPersonnelles.html";
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(intent);
	}

	private void startAppli(JSONObject jsonResponse) {
		try {
			int version = jsonResponse.getInt("version");
			if (version == this.version) {
				sessionManager.initializeSession(jsonResponse);

				if (AndyUtils.hasOldCtrl(this) && AndyUtils.isNetworkAvailable(this)) {
					Intent intent = new Intent(this, SynchronizeActivity.class);
					intent.putExtra(SelectActivity.SELECT_ACTIVITY_EXTRA, jsonResponse.toString());
					makeMainActivityLauncher.launch(intent);
				} else {
					Intent intent = new Intent(this, SelectActivity.class);
					intent.putExtra(SelectActivity.SELECT_ACTIVITY_EXTRA, jsonResponse.toString());
					startActivity(intent);
				}
			} else {
				runOnUiThread(this::openVersion);
			}
		} catch (JSONException e) {
			runOnUiThread(() -> Toast.makeText(this, "Erreur de traitement du JSON", Toast.LENGTH_SHORT).show());
		}
	}

	private void testIdentified() {
		permissionManager.checkMainActivityPermissions(new PermissionManager.PermissionResultCallback() {
			@Override
			public void onResult(boolean granted) {
				if (granted) {
					String currentIdMbr = sessionManager.getIdMbr();
					String currentAdrMac = Build.FINGERPRINT;

					loginManager.checkVersion(currentIdMbr, currentAdrMac)
							.thenAccept(jsonResponse -> handleVersionCheck(jsonResponse))
							.exceptionally(ex -> {
								runOnUiThread(() -> {
									Toast.makeText(MainActivity.this, R.string.mess_timeout, Toast.LENGTH_SHORT).show();
									openConexion();
								});
								return null;
							});
				}
			}

			@Override
			public void onError(Exception e) {
				runOnUiThread(() -> {
					Toast.makeText(MainActivity.this, "Erreur lors de la vérification des permissions", Toast.LENGTH_SHORT)
							.show();
					openConexion();
				});
			}
		});
	}

	private void openConexion() {
		idMbr = "new";
		isConnected = false;

		try {
			// Utiliser setLoginFields avec validation
			uiManager.setLoginFields(userName != null ? userName : "", password);
			uiManager.showConnectLayout();
			uiManager.showWaitIndicator(false);
		} catch (MainActivityUIManager.UIValidationException e) {
			logger.error("Erreur lors de l'initialisation des champs de connexion", e);
			// En cas d'erreur, réinitialiser les champs
			userName = "";
			password = "";
			uiManager.clearLoginFields();
		}
	}

	private void openDeco() {
		// Affiche l'écran de déconnexion

		uiManager.showDisconnectLayout();
	}

	private void openVersion() {
		// Affiche les informations de version

		uiManager.showVersionLayout();
	}

	private void showWait(boolean b) {
		uiManager.showWaitIndicator(b);
	}

	@Override
	public void onLoginSuccess(JSONObject response) {
		runOnUiThread(() -> {
			try {
				startAppli(response);
			} catch (Exception e) {
				Toast.makeText(this, "Erreur lors du démarrage", Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onLoginFailure(String message) {
		runOnUiThread(() -> {
			try {
				uiManager.showToast(message, true);
				uiManager.showWaitIndicator(false);
				isConnected = false;

				// Réinitialiser les champs en cas d'échec
				if (uiManager.getCurrentLayout() == MainActivityUIManager.LAYOUT_CONNECT) {
					uiManager.clearLoginFields();
				}
			} catch (Exception e) {
				logger.error("Erreur lors du traitement de l'échec de connexion", e);
			}
		});
	}

	@Override
	public void onNetworkError(Exception e) {
		runOnUiThread(() -> {
			Toast.makeText(this, R.string.mess_timeout, Toast.LENGTH_SHORT).show();
			openConexion();
		});
	}

	@Override
	public void onSecurityAlert(String reason, Map<String, Object> details) {

	}

	@Override
	public void onSecurityAlert(String reason, Map<String, Object> details) {
		runOnUiThread(() -> {
			Toast.makeText(this, "Alerte de sécurité: " + reason, Toast.LENGTH_LONG).show();
			logger.warn("Alerte de sécurité: {} - {}", reason, details);
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		loginManager.dispose();
	}

	// ********* Getters and Setters

	public static MainActivity getMainActivity() {
		return instance;
	}

	public SharedPreferences getPreferences() {
		return Preferences;
	}

	public String getIdMbr() {
		return idMbr;
	}

	private void handleVersionCheck(JSONObject response) {
		try {
			if (response == null) {
				throw new JSONException("La réponse du serveur est vide");
			}

			if (!response.has("status")) {
				throw new JSONException("Format de réponse invalide - status manquant");
			}

			boolean status = response.getBoolean("status");

			if (status) {
				// Vérification de la version
				if (!response.has("version")) {
					throw new JSONException("Version non spécifiée dans la réponse");
				}

				int serverVersion = response.getInt("version");
				if (serverVersion != version) {
					runOnUiThread(() -> {
						Toast.makeText(this,
								getString(R.string.err_version_mismatch, version, serverVersion),
								Toast.LENGTH_LONG).show();
						openVersion();
					});
					return;
				}

				// Démarrage de l'application
				startAppli(response);

			} else {
				// Gestion des différents cas d'erreur
				String errorCode = response.optString("errorCode", "UNKNOWN");
				String message = getErrorMessage(errorCode, response.optString("message"));

				runOnUiThread(() -> {
					Toast.makeText(this, message, Toast.LENGTH_LONG).show();
					openConexion();
				});
			}
		} catch (JSONException e) {
			logger.error("Erreur lors du traitement de la réponse JSON", e);
			runOnUiThread(() -> {
				Toast.makeText(this,
						getString(R.string.err_json_parse, e.getMessage()),
						Toast.LENGTH_SHORT).show();
				openConexion();
			});
		} catch (Exception e) {
			logger.error("Erreur inattendue lors de la vérification de version", e);
			runOnUiThread(() -> {
				Toast.makeText(this,
						getString(R.string.err_unexpected, e.getMessage()),
						Toast.LENGTH_SHORT).show();
				openConexion();
			});
		}
	}

	private String getErrorMessage(String errorCode, String defaultMessage) {
		switch (errorCode) {
			case "VERSION_OUTDATED":
				return getString(R.string.err_version_outdated);
			case "DEVICE_NOT_RECOGNIZED":
				return getString(R.string.err_device_not_recognized);
			case "SESSION_EXPIRED":
				return getString(R.string.err_session_expired);
			case "NETWORK_ERROR":
				return getString(R.string.err_network);
			case "SERVER_ERROR":
				return getString(R.string.err_server);
			default:
				return defaultMessage != null ? defaultMessage : getString(R.string.err_unknown);
		}
	}

	// Implémentation de LoginInputValidator.ValidationCallback
	@Override
	public void onValidationError(String message) {
		runOnUiThread(() -> {
			uiManager.showToast(message, true);
			uiManager.showWaitIndicator(false);
		});
	}

	@Override
	public void onValidationSuccess() {
		// Optional: peut être utilisé pour des actions spécifiques après validation
		// réussie
	}

	// Implémentation de UIErrorHandler
	@Override
	public void onUIError(MainActivityUIManager.UIException error) {
		runOnUiThread(() -> {
			uiManager.showToast("Erreur UI: " + error.getMessage(), true);
			logger.error("Erreur UI générique", error);
		});
	}

	@Override
	public void onLayoutError(String message) {
		runOnUiThread(() -> {
			uiManager.showToast("Erreur de layout: " + message, true);
			logger.error("Erreur de layout: {}", message);
		});
	}

	@Override
	public void onStateError(String message) {
		runOnUiThread(() -> {
			uiManager.showToast("Erreur d'état: " + message, true);
			logger.error("Erreur d'état: {}", message);
			// Réinitialiser l'état en cas d'erreur grave
			uiManager.showWaitIndicator(false);
		});
	}

	// Implémentation de UIEventListener
	@Override
	public void onLayoutChanged(@LayoutType int newLayout, @LayoutType int oldLayout) {
		logger.debug("Changement de layout: {} -> {}", oldLayout, newLayout);
		// Logique spécifique au changement de layout
	}

	@Override
	public void onLoadingStateChanged(boolean isLoading) {
		// Mise à jour de l'UI en fonction de l'état de chargement
		if (isLoading) {
			disableUserInteraction();
		} else {
			enableUserInteraction();
		}
	}

	@Override
	public void onInputChanged(String username, String password) {
		// Validation en temps réel si nécessaire
		try {
			inputValidator.validateInputs(username, password);
		} catch (ValidationException e) {
			// Gérer les erreurs de validation sans bloquer l'utilisateur
		}
	}

	@Override
	public void onRgpdStateChanged(boolean accepted) {
		// Mettre à jour l'état du bouton de connexion
		binding.mainActivityConnectBtn.setEnabled(accepted);
	}

	@Override
	public void onClearFields() {
		// Actions après nettoyage des champs
		isConnected = false;
		userName = "";
		password = "";
	}

	@Override
	public void onThemeChanged(boolean isDarkMode) {
		// Mettre à jour les éléments UI spécifiques à MainActivity si nécessaire
		logger.debug("Thème changé: mode {}", isDarkMode ? "sombre" : "clair");
	}

	private void disableUserInteraction() {
		binding.mainActivityConnectBtn.setEnabled(false);
		binding.mainActivityUsernameTxt.setEnabled(false);
		binding.mainActivityPasswordTxt.setEnabled(false);
	}

	private void enableUserInteraction() {
		binding.mainActivityConnectBtn.setEnabled(true);
		binding.mainActivityUsernameTxt.setEnabled(true);
		binding.mainActivityPasswordTxt.setEnabled(true);
	}
}
