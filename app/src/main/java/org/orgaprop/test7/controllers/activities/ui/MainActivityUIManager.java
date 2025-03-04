package org.orgaprop.test7.controllers.activities.ui;

import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import org.orgaprop.test7.databinding.ActivityMainBinding;
import org.orgaprop.test7.controllers.activities.MainActivity;
import androidx.annotation.IntDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import android.text.Editable;
import android.text.TextWatcher;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.widget.Button;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class MainActivityUIManager implements SwipeGestureDetector.SwipeCallback {
	private static final Logger logger = LoggerFactory.getLogger(MainActivityUIManager.class);
	private final UILogger uiLogger;

	@IntDef({ LAYOUT_CONNECT, LAYOUT_DISCONNECT, LAYOUT_VERSION })
	@Retention(RetentionPolicy.SOURCE)
	public @interface LayoutType {
	}

	public static final int LAYOUT_CONNECT = 0;
	public static final int LAYOUT_DISCONNECT = 1;
	public static final int LAYOUT_VERSION = 2;

	// Remplacer la référence directe par une WeakReference
	private final WeakReference<MainActivity> activityRef;
	private final WeakReference<ActivityMainBinding> bindingRef;
	private final UIState uiState;
	private UIErrorHandler errorHandler;
	private final UIErrorTracker errorTracker;
	private final ThemeManager themeManager;
	private final UIMetrics uiMetrics;
	private final UIAnalytics analytics;
	private static final String INTERACTION_INPUT = "input";
	private static final String INTERACTION_BUTTON = "button";
	private static final String INTERACTION_CHECKBOX = "checkbox";

	private static final long ANIMATION_DURATION = 200; // durée en ms
	private boolean isAnimating = false;

	private final Map<Integer, ViewState> layoutStates = new HashMap<>();

	private static class ViewState {
		final float alpha;
		final int visibility;
		final boolean enabled;

		ViewState(View view) {
			this.alpha = view.getAlpha();
			this.visibility = view.getVisibility();
			this.enabled = view.isEnabled();
		}
	}

	private static class UIState {
		@LayoutType
		private int currentLayout = LAYOUT_CONNECT;
		private boolean isLoading;
		private String lastError;
		private long lastStateChangeTime;

		void setLayout(@LayoutType int layout) {
			currentLayout = layout;
			lastStateChangeTime = System.currentTimeMillis();
		}

		void setLoading(boolean loading) {
			isLoading = loading;
		}

		void setError(String error) {
			lastError = error;
		}
	}

	// Handler pour les opérations UI
	private final Handler uiHandler;

	// Locks pour différentes ressources
	private final Object layoutLock = new Object();
	private final Object stateLock = new Object();
	private final Object metricsLock = new Object();
	private final Object animationLock = new Object();
	private final ReentrantReadWriteLock themeLock = new ReentrantReadWriteLock();

	// Protection des listeners
	private final CopyOnWriteArrayList<UIEventListener> eventListeners = new CopyOnWriteArrayList<>();

	private final UICompatibilityManager compatManager;

	private GestureDetector gestureDetector;
	private boolean swipeEnabled = true;

	private final ViewStateCache viewStateCache;
	private static final long STATE_CACHE_DURATION = 5000; // 5 secondes

	private final UIAccessibilityManager accessibilityManager;
	private final UIPerformanceTracker performanceTracker;

	public MainActivityUIManager(MainActivity activity, ActivityMainBinding binding) {
		// Utilisation de WeakReference pour éviter les fuites
		this.activityRef = new WeakReference<>(activity);
		this.bindingRef = new WeakReference<>(binding);

		// Initialisation du Handler sur le thread UI
		this.uiHandler = new Handler(Looper.getMainLooper());

		this.uiState = new UIState();
		this.errorTracker = new UIErrorTracker();
		this.themeManager = new ThemeManager();
		this.uiMetrics = new UIMetrics();
		this.analytics = new UIAnalytics();
		this.uiLogger = new UILogger("MainActivityUI");
		this.compatManager = new UICompatibilityManager();
		this.viewStateCache = new ViewStateCache();
		this.accessibilityManager = new UIAccessibilityManager();
		this.performanceTracker = new UIPerformanceTracker();

		initializeTheme();
		setupAnalytics();
		setupGestureSupport();
		setupAccessibility();
	}

	// Méthode sécurisée pour accéder à l'Activity
	private MainActivity getActivity() {
		MainActivity activity = activityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
			throw new IllegalStateException("Activity n'est plus valide");
		}
		return activity;
	}

	// Méthode sécurisée pour accéder au Binding
	private ActivityMainBinding getBinding() {
		ActivityMainBinding binding = bindingRef.get();
		if (binding == null) {
			throw new IllegalStateException("Binding n'est plus valide");
		}
		return binding;
	}

	// Méthode pour vérifier la validité du contexte
	private void validateContext() {
		try {
			getActivity();
			getBinding();
		} catch (IllegalStateException e) {
			logger.error("Contexte invalide", e);
			dispose();
			throw e;
		}
	}

	/**
	 * Gère l'animation entre deux layouts
	 */
	private void animateLayoutTransition(View showView, View hideView, @LayoutType int newLayout) {
		String animationId = "animation_" + System.currentTimeMillis();
		performanceTracker.startOperation(UIPerformanceTracker.OperationType.ANIMATION, animationId);

		synchronized (animationLock) {
			if (isAnimating) {
				return;
			}
			isAnimating = true;
		}

		// Utiliser le gestionnaire de compatibilité pour les animations
		compatManager.createFadeAnimation(hideView, 0f)
				.withEndAction(() -> {
					hideView.setVisibility(View.GONE);
					showView.setVisibility(View.VISIBLE);

					compatManager.createFadeAnimation(showView, 1f)
							.withEndAction(() -> {
								synchronized (animationLock) {
									isAnimating = false;
								}
								updateUIState(newLayout);
								notifyLayoutChanged(newLayout);
								performanceTracker.endOperation(UIPerformanceTracker.OperationType.ANIMATION, animationId);
							})
							.start();
				})
				.start();
	}

	@Override
	public void showConnectLayout() {
		String operationId = "layout_change_" + System.currentTimeMillis();
		performanceTracker.startOperation(UIPerformanceTracker.OperationType.LAYOUT_CHANGE, operationId);

		synchronized (layoutLock) {
			validateContext();
			try {
				ActivityMainBinding binding = getBinding();
				validateUIState(LAYOUT_CONNECT);
				if (uiState.currentLayout == LAYOUT_CONNECT) {
					return;
				}

				// Vérifier si l'état a changé avant d'effectuer la transition
				if (!viewStateCache.hasStateChanged(R.id.mainActivityConnectLyt, binding.mainActivityConnectLyt)) {
					logger.debug("État du layout connexion inchangé, utilisation du cache");
					restoreFromCache();
					return;
				}

				View showView = binding.mainActivityConnectLyt;
				View hideView;

				if (uiState.currentLayout == LAYOUT_DISCONNECT) {
					hideView = binding.mainActivityDecoLyt;
				} else {
					hideView = binding.mainActivityVersionLyt;
				}

				executeTransition(showView, hideView, LAYOUT_CONNECT);
				logger.debug("Animation vers layout connexion");
				synchronized (metricsLock) {
					uiMetrics.recordLayoutChange(LAYOUT_CONNECT);
				}
				analytics.trackEvent(
						UIAnalytics.EventType.VIEW_SHOWN,
						"show_connect_layout",
						Map.of("previousLayout", uiState.currentLayout));

				Map<String, Object> metadata = new HashMap<>();
				metadata.put("previousLayout", uiState.currentLayout);
				metadata.put("isAnimating", isAnimating);
				metadata.put("loadingState", uiState.isLoading);
				uiLogger.logUIEvent(UILogger.LogLevel.INFO, "Affichage layout connexion", metadata);

				// Mettre en cache le nouvel état
				cacheCurrentLayout();

				if (accessibilityManager.isAccessibilityEnabled()) {
					accessibilityManager.announceForAccessibility(
							binding.mainActivityConnectLyt,
							"Affichage de l'écran de connexion");
				}

				performanceTracker.endOperation(UIPerformanceTracker.OperationType.LAYOUT_CHANGE, operationId);

			} catch (UIException e) {
				Map<String, Object> errorMetadata = new HashMap<>();
				errorMetadata.put("errorType", e.getErrorType());
				errorMetadata.put("currentLayout", uiState.currentLayout);
				uiLogger.logUIEvent(UILogger.LogLevel.ERROR, "Erreur affichage layout connexion", errorMetadata);
				handleUIError(e);
				performanceTracker.endOperation(UIPerformanceTracker.OperationType.LAYOUT_CHANGE, operationId);
			}
		}
	}

	@Override
	public void showDisconnectLayout() {
		synchronized (layoutLock) {
			try {
				validateUIState(LAYOUT_DISCONNECT);
				if (uiState.currentLayout == LAYOUT_DISCONNECT) {
					return;
				}

				ActivityMainBinding binding = getBinding();
				View showView = binding.mainActivityDecoLyt;
				View hideView;

				if (uiState.currentLayout == LAYOUT_CONNECT) {
					hideView = binding.mainActivityConnectLyt;
				} else {
					hideView = binding.mainActivityVersionLyt;
				}

				animateLayoutTransition(showView, hideView, LAYOUT_DISCONNECT);
				logger.debug("Animation vers layout déconnexion");
				analytics.trackEvent(
						UIAnalytics.EventType.VIEW_SHOWN,
						"show_disconnect_layout",
						Map.of("previousLayout", uiState.currentLayout));

			} catch (UIException e) {
				handleUIError(e);
			}
		}
	}

	@Override
	public void showVersionLayout() {
		synchronized (layoutLock) {
			try {
				validateUIState(LAYOUT_VERSION);
				if (uiState.currentLayout == LAYOUT_VERSION) {
					return;
				}

				ActivityMainBinding binding = getBinding();
				View showView = binding.mainActivityVersionLyt;
				View hideView;

				if (uiState.currentLayout == LAYOUT_CONNECT) {
					hideView = binding.mainActivityConnectLyt;
				} else {
					hideView = binding.mainActivityDecoLyt;
				}

				animateLayoutTransition(showView, hideView, LAYOUT_VERSION);
				logger.debug("Animation vers layout version");

			} catch (UIException e) {
				handleUIError(e);
			}
		}
	}

	/**
	 * Animation du loader
	 */
	@Override
	public void showWaitIndicator(boolean show) {
		synchronized (stateLock) {
			if (uiState.isLoading == show) {
				return;
			}

			ActivityMainBinding binding = getBinding();
			View waitView = binding.mainActivityWaitImg;

			if (show) {
				waitView.setVisibility(View.VISIBLE);
				compatManager.createFadeAnimation(waitView, 1f).start();
			} else {
				compatManager.createFadeAnimation(waitView, 0f)
						.withEndAction(() -> waitView.setVisibility(View.INVISIBLE))
						.start();
			}

			uiState.setLoading(show);

			if (uiEventListener != null) {
				uiEventListener.onLoadingStateChanged(show);
			}

			logger.debug("Animation indicateur d'attente: {}", show);
			if (testSupport != null) {
				testSupport.getCurrentUIState().put("waitIndicatorShown", show);
			}

			if (accessibilityManager.isAccessibilityEnabled()) {
				String message = show ? "Chargement en cours" : "Chargement terminé";
				accessibilityManager.announceForAccessibility(
						binding.mainActivityWaitImg,
						message);
			}
		}
	}

	@LayoutType
	public int getCurrentLayout() {
		synchronized (stateLock) {
			return uiState.currentLayout;
		}
	}

	public boolean isLoading() {
		synchronized (stateLock) {
			return uiState.isLoading;
		}
	}

	public String getLastError() {
		synchronized (stateLock) {
			return uiState.lastError;
		}
	}

	public long getLastStateChangeTime() {
		synchronized (stateLock) {
			return uiState.lastStateChangeTime;
		}
	}

	/**
	 * Exception personnalisée pour les erreurs de validation UI
	 */
	public static class UIValidationException extends RuntimeException {
		public UIValidationException(String message) {
			super(message);
		}
	}

	/**
	 * Exception spécifique pour les erreurs d'interface utilisateur
	 */
	public static class UIException extends RuntimeException {
		private final UIErrorType errorType;

		public UIException(String message, UIErrorType errorType) {
			super(message);
			this.errorType = errorType;
		}

		public UIErrorType getErrorType() {
			return errorType;
		}
	}

	/**
	 * Types d'erreurs UI possibles
	 */
	public enum UIErrorType {
		INVALID_STATE,
		VALIDATION_ERROR,
		LAYOUT_ERROR,
		INPUT_ERROR
	}

	/**
	 * Interface pour gérer les événements d'erreur UI
	 */
	public interface UIErrorHandler {
		void onUIError(UIException error);

		void onLayoutError(String message);

		void onValidationError(String message);

		void onStateError(String message);
	}

	/**
	 * Interface pour écouter les événements UI
	 */
	public interface UIEventListener {
		void onLayoutChanged(@LayoutType int newLayout, @LayoutType int oldLayout);

		void onLoadingStateChanged(boolean isLoading);

		void onInputChanged(String username, String password);

		void onRgpdStateChanged(boolean accepted);

		void onValidationSuccess();

		void onClearFields();

		void onThemeChanged(boolean isDarkMode);
	}

	private UIEventListener uiEventListener;

	public void setUIEventListener(UIEventListener listener) {
		this.uiEventListener = listener;
	}

	public void addUIEventListener(UIEventListener listener) {
		if (listener != null) {
			eventListeners.add(listener);
		}
	}

	public void removeUIEventListener(UIEventListener listener) {
		eventListeners.remove(listener);
	}

	protected void notifyListeners(Consumer<UIEventListener> action) {
		for (UIEventListener listener : eventListeners) {
			try {
				action.accept(listener);
			} catch (Exception e) {
				logger.error("Erreur lors de la notification du listener", e);
			}
		}
	}

	/**
	 * Classe pour suivre les erreurs UI
	 */
	private static class UIErrorTracker {
		private int errorCount = 0;
		private long lastErrorTime = 0;
		private String lastErrorMessage = null;
		private final Map<UIErrorType, Integer> errorTypeCount = new HashMap<>();

		void trackError(UIException error) {
			errorCount++;
			lastErrorTime = System.currentTimeMillis();
			lastErrorMessage = error.getMessage();
			errorTypeCount.merge(error.getErrorType(), 1, Integer::sum);
		}

		void reset() {
			errorCount = 0;
			lastErrorTime = 0;
			lastErrorMessage = null;
			errorTypeCount.clear();
		}
	}

	/**
	 * Valide l'état de l'UI avant un changement
	 */
	private void validateUIState(int newLayout) throws UIException {
		synchronized (stateLock) {
			if (uiState.isLoading && newLayout != uiState.currentLayout) {
				throw new UIException(
						"Impossible de changer de layout pendant le chargement",
						UIErrorType.INVALID_STATE);
			}
		}
	}

	/**
	 * Gère les erreurs UI de manière centralisée
	 */
	private void handleUIError(UIException error) {
		Map<String, Object> errorMetadata = new HashMap<>();
		errorMetadata.put("errorType", error.getErrorType());
		errorMetadata.put("errorCount", errorTracker.errorCount);
		errorMetadata.put("lastError", errorTracker.lastErrorMessage);
		uiLogger.logUIEvent(UILogger.LogLevel.ERROR, "Erreur UI détectée", errorMetadata);

		errorTracker.trackError(error);

		if (errorHandler != null) {
			switch (error.getErrorType()) {
				case LAYOUT_ERROR:
					errorHandler.onLayoutError(error.getMessage());
					break;
				case VALIDATION_ERROR:
					errorHandler.onValidationError(error.getMessage());
					break;
				case INVALID_STATE:
					errorHandler.onStateError(error.getMessage());
					break;
				default:
					errorHandler.onUIError(error);
			}
		}
	}

	/**
	 * Récupère les statistiques d'erreur
	 */
	public Map<String, Object> getErrorStats() {
		synchronized (metricsLock) {
			Map<String, Object> stats = new HashMap<>();
			stats.put("errorCount", errorTracker.errorCount);
			stats.put("lastErrorTime", errorTracker.lastErrorTime);
			stats.put("lastErrorMessage", errorTracker.lastErrorMessage);
			stats.put("errorTypeDistribution", new HashMap<>(errorTracker.errorTypeCount));
			return stats;
		}
	}

	/**
	 * Définit les champs de connexion avec validation
	 * 
	 * @throws UIValidationException si le nom d'utilisateur est null
	 */
	public void setLoginFields(@NonNull String username, @Nullable String password) {
		Objects.requireNonNull(username, "Le nom d'utilisateur ne peut pas être null");
		validateUsername(username);

		ActivityMainBinding binding = getBinding();
		binding.mainActivityUsernameTxt.setText(username);
		binding.mainActivityPasswordTxt.setText(password != null ? password : "");

		logger.debug("Champs de connexion définis - utilisateur: {}", username);
	}

	/**
	 * Valide le format du nom d'utilisateur
	 */
	private void validateUsername(String username) {
		if (username.trim().isEmpty()) {
			throw new UIValidationException("Le nom d'utilisateur ne peut pas être vide");
		}
		if (username.length() < 3) {
			throw new UIValidationException("Le nom d'utilisateur doit contenir au moins 3 caractères");
		}
		if (!username.matches("^[a-zA-Z0-9._-]+$")) {
			throw new UIValidationException("Le nom d'utilisateur contient des caractères non autorisés");
		}
	}

	/**
	 * Récupère le nom d'utilisateur avec validation
	 */
	@NonNull
	public String getUsername() {
		ActivityMainBinding binding = getBinding();
		String username = binding.mainActivityUsernameTxt.getText().toString();
		validateUsername(username);
		return username;
	}

	/**
	 * Récupère le mot de passe avec validation
	 */
	@NonNull
	public String getPassword() {
		ActivityMainBinding binding = getBinding();
		String password = binding.mainActivityPasswordTxt.getText().toString();
		if (password.trim().isEmpty()) {
			throw new UIValidationException("Le mot de passe ne peut pas être vide");
		}
		return password;
	}

	/**
	 * Vérifie si le RGPD est accepté
	 * 
	 * @throws UIValidationException si la case n'est pas cochée
	 */
	public boolean isRgpdAccepted() {
		ActivityMainBinding binding = getBinding();
		boolean isAccepted = binding.mainActivityRgpdChx.isChecked();
		if (!isAccepted) {
			throw new UIValidationException("Vous devez accepter les conditions RGPD");
		}
		return true;
	}

	/**
	 * Vérifie la validité de tous les champs
	 * 
	 * @return true si tous les champs sont valides
	 * @throws UIValidationException si un champ est invalide
	 */
	public boolean validateAllFields() {
		String validationId = "validation_" + System.currentTimeMillis();
		performanceTracker.startOperation(UIPerformanceTracker.OperationType.VALIDATION, validationId);

		synchronized (stateLock) {
			try {
				String username = getUsername();
				String password = getPassword();
				boolean rgpdAccepted = isRgpdAccepted();

				logger.debug("Validation des champs réussie");

				Map<String, Object> validationMetadata = new HashMap<>();
				validationMetadata.put("hasUsername", !username.isEmpty());
				validationMetadata.put("rgpdAccepted", rgpdAccepted);
				validationMetadata.put("currentLayout", uiState.currentLayout);
				uiLogger.logUIEvent(UILogger.LogLevel.INFO, "Validation des champs réussie", validationMetadata);

				if (uiEventListener != null) {
					uiEventListener.onValidationSuccess();
				}

				performanceTracker.endOperation(UIPerformanceTracker.OperationType.VALIDATION, validationId);
				return true;
			} catch (UIValidationException e) {
				logger.warn("Échec de la validation des champs: {}", e.getMessage());
				Map<String, Object> errorMetadata = new HashMap<>();
				errorMetadata.put("validationType", "FIELD_VALIDATION");
				errorMetadata.put("errorMessage", e.getMessage());
				uiLogger.logUIEvent(UILogger.LogLevel.WARN, "Échec de la validation des champs", errorMetadata);
				performanceTracker.endOperation(UIPerformanceTracker.OperationType.VALIDATION, validationId);
				throw e;
			}
		}
	}

	public void clearLoginFields() {
		ActivityMainBinding binding = getBinding();
		binding.mainActivityUsernameTxt.setText("");
		binding.mainActivityPasswordTxt.setText("");
		binding.mainActivityRgpdChx.setChecked(false);

		if (uiEventListener != null) {
			uiEventListener.onClearFields();
		}
	}

	public boolean areLoginFieldsValid() {
		return !getUsername().isEmpty() && !getPassword().isEmpty();
	}

	// Amélioration de showToast pour éviter les fuites
	public void showToast(String message, boolean longDuration) {
		runOnUiThread(() -> {
			try {
				MainActivity activity = getActivity();
				Toast.makeText(activity, message,
						longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
						.show();
			} catch (IllegalStateException e) {
				logger.warn("Impossible d'afficher le toast: contexte invalide");
			}
		});
	}

	public void showToast(int resourceId, boolean longDuration) {
		runOnUiThread(() -> {
			try {
				MainActivity activity = getActivity();
				Toast.makeText(activity, resourceId,
						longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
						.show();
			} catch (IllegalStateException e) {
				logger.warn("Impossible d'afficher le toast: contexte invalide");
			}
		});
	}

	public void setupInputListeners() {
		ActivityMainBinding binding = getBinding();
		binding.mainActivityUsernameTxt.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				notifyInputChanged();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});

		binding.mainActivityPasswordTxt.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				notifyInputChanged();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});

		binding.mainActivityRgpdChx.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (uiEventListener != null) {
				uiEventListener.onRgpdStateChanged(isChecked);
			}
			uiMetrics.recordInteraction(INTERACTION_CHECKBOX);
		});

		// Ajouter des listeners pour les boutons
		setupButtonMetrics(binding.mainActivityConnectBtn);
		setupButtonMetrics(binding.mainActivityDecoBtn);
		setupButtonMetrics(binding.mainActivityRobotBtn);
		setupButtonMetrics(binding.mainActivityMailBtn);
	}

	private void notifyInputChanged() {
		String inputId = "input_" + System.currentTimeMillis();
		performanceTracker.startOperation(UIPerformanceTracker.OperationType.INPUT_PROCESSING, inputId);

		ActivityMainBinding binding = getBinding();
		if (uiEventListener != null) {
			uiEventListener.onInputChanged(
					binding.mainActivityUsernameTxt.getText().toString(),
					binding.mainActivityPasswordTxt.getText().toString());
		}
		uiMetrics.recordInteraction(INTERACTION_INPUT);
		analytics.trackEvent(
				UIAnalytics.EventType.INPUT_CHANGED,
				"input_changed",
				Map.of(
						"hasUsername", !binding.mainActivityUsernameTxt.getText().toString().isEmpty(),
						"hasPassword", !binding.mainActivityPasswordTxt.getText().toString().isEmpty()));

		String username = binding.mainActivityUsernameTxt.getText().toString();
		String password = binding.mainActivityPasswordTxt.getText().toString();

		Map<String, Object> inputMetadata = new HashMap<>();
		inputMetadata.put("hasUsername", !username.isEmpty());
		inputMetadata.put("hasPassword", !password.isEmpty());
		inputMetadata.put("inputType", "TEXT_CHANGE");
		uiLogger.logUIEvent(UILogger.LogLevel.DEBUG, "Modification des champs de saisie", inputMetadata);

		performanceTracker.endOperation(UIPerformanceTracker.OperationType.INPUT_PROCESSING, inputId);
	}

	private void setupButtonMetrics(View button) {
		button.setOnClickListener(v -> {
			uiMetrics.recordInteraction(INTERACTION_BUTTON);
			// Garder le comportement original du bouton
			v.performClick();
		});
	}

	/**
	 * Classe qui encapsule l'état complet de l'UI
	 */
	private static class CompleteUIState {
		@LayoutType
		private final int currentLayout;
		private final boolean isLoading;
		private final String username;
		private final boolean rgpdAccepted;
		private final Map<String, Object> errorStats;
		private final long lastStateChangeTime;

		CompleteUIState(UIState uiState, String username, boolean rgpdAccepted,
				Map<String, Object> errorStats) {
			this.currentLayout = uiState.currentLayout;
			this.isLoading = uiState.isLoading;
			this.username = username;
			this.rgpdAccepted = rgpdAccepted;
			this.errorStats = errorStats;
			this.lastStateChangeTime = uiState.lastStateChangeTime;
		}
	}

	/**
	 * Sauvegarde l'état complet de l'UI
	 */
	public Bundle saveState() {
		Bundle state = new Bundle();

		try {
			// Sauvegarder l'état du layout
			state.putInt("currentLayout", uiState.currentLayout);
			state.putBoolean("isLoading", uiState.isLoading);
			state.putLong("lastStateChangeTime", uiState.lastStateChangeTime);

			// Sauvegarder les entrées utilisateur
			ActivityMainBinding binding = getBinding();
			state.putString("username", binding.mainActivityUsernameTxt.getText().toString());
			state.putBoolean("rgpdAccepted", binding.mainActivityRgpdChx.isChecked());

			// Sauvegarder les statistiques d'erreur
			Bundle errorStatsBundle = new Bundle();
			Map<String, Object> errorStats = getErrorStats();
			for (Map.Entry<String, Object> entry : errorStats.entrySet()) {
				if (entry.getValue() instanceof Integer) {
					errorStatsBundle.putInt(entry.getKey(), (Integer) entry.getValue());
				} else if (entry.getValue() instanceof Long) {
					errorStatsBundle.putLong(entry.getKey(), (Long) entry.getValue());
				} else if (entry.getValue() instanceof String) {
					errorStatsBundle.putString(entry.getKey(), (String) entry.getValue());
				}
			}
			state.putBundle("errorStats", errorStatsBundle);

			// Sauvegarder l'état du cache avant la mise en veille
			cacheCurrentLayout();

			// Ajouter les métriques de performance
			Map<String, Object> performanceMetrics = performanceTracker.getPerformanceReport();
			state.putSerializable("performance_metrics", new HashMap<>(performanceMetrics));

			logger.debug("État UI sauvegardé avec succès");
			return state;

		} catch (Exception e) {
			logger.error("Erreur lors de la sauvegarde de l'état UI", e);
			throw new UIException("Échec de la sauvegarde de l'état", UIErrorType.INVALID_STATE);
		}
	}

	/**
	 * Restaure l'état complet de l'UI
	 */
	public void restoreState(Bundle state) {
		if (state == null) {
			logger.debug("Aucun état à restaurer");
			return;
		}

		try {
			// Restaurer l'état du layout
			@LayoutType
			int savedLayout = state.getInt("currentLayout", LAYOUT_CONNECT);
			switch (savedLayout) {
				case LAYOUT_CONNECT:
					showConnectLayout();
					break;
				case LAYOUT_DISCONNECT:
					showDisconnectLayout();
					break;
				case LAYOUT_VERSION:
					showVersionLayout();
					break;
			}

			// Restaurer l'état de chargement
			showWaitIndicator(state.getBoolean("isLoading", false));

			// Restaurer les entrées utilisateur
			String savedUsername = state.getString("username", "");
			setLoginFields(savedUsername, ""); // Ne pas restaurer le mot de passe pour la sécurité
			ActivityMainBinding binding = getBinding();
			binding.mainActivityRgpdChx.setChecked(state.getBoolean("rgpdAccepted", false));

			// Restaurer les statistiques d'erreur
			Bundle errorStatsBundle = state.getBundle("errorStats");
			if (errorStatsBundle != null) {
				errorTracker.reset();
				for (String key : errorStatsBundle.keySet()) {
					Object value = errorStatsBundle.get(key);
					if (value != null) {
						// Restaurer chaque statistique
						if (value instanceof Integer) {
							errorTypeCount.put(key, (Integer) value);
						}
					}
				}
			}

			// Mettre à jour l'horodatage
			uiState.lastStateChangeTime = state.getLong("lastStateChangeTime", System.currentTimeMillis());

			// Restaurer l'état du cache après la reprise
			restoreFromCache();

			logger.debug("État UI restauré avec succès");

			// Notifier les listeners
			if (uiEventListener != null) {
				uiEventListener.onLayoutChanged(savedLayout, uiState.currentLayout);
				uiEventListener.onInputChanged(savedUsername, "");
				uiEventListener.onRgpdStateChanged(state.getBoolean("rgpdAccepted", false));
			}

		} catch (Exception e) {
			logger.error("Erreur lors de la restauration de l'état UI", e);
			throw new UIException("Échec de la restauration de l'état", UIErrorType.INVALID_STATE);
		}
	}

	/**
	 * Gestionnaire du thème UI
	 */
	private class ThemeManager {
		private boolean isDarkMode = false;
		private final Map<Integer, Integer> darkModeColors = new HashMap<>();
		private final Map<Integer, Integer> lightModeColors = new HashMap<>();

		ThemeManager() {
			initializeColorMaps();
		}

		private void initializeColorMaps() {
			// Couleurs mode sombre
			darkModeColors.put(R.color.text_primary, R.color.text_primary_dark);
			darkModeColors.put(R.color.text_secondary, R.color.text_secondary_dark);
			darkModeColors.put(R.color.background_primary, R.color.background_primary_dark);
			darkModeColors.put(R.color.background_secondary, R.color.background_secondary_dark);
			darkModeColors.put(R.color.button_background, R.color.button_background_dark);
			darkModeColors.put(R.color.button_text, R.color.button_text_dark);

			// Couleurs mode clair
			lightModeColors.put(R.color.text_primary, R.color.text_primary_light);
			lightModeColors.put(R.color.text_secondary, R.color.text_secondary_light);
			lightModeColors.put(R.color.background_primary, R.color.background_primary_light);
			lightModeColors.put(R.color.background_secondary, R.color.background_secondary_light);
			lightModeColors.put(R.color.button_background, R.color.button_background_light);
			lightModeColors.put(R.color.button_text, R.color.button_text_light);
		}

		void updateTheme(boolean darkMode) {
			if (isDarkMode == darkMode)
				return;

			isDarkMode = darkMode;
			applyTheme();
			logger.debug("Thème mis à jour: mode {}", darkMode ? "sombre" : "clair");
		}

		private void applyTheme() {
			Map<Integer, Integer> currentColors = isDarkMode ? darkModeColors : lightModeColors;

			// Mise à jour des couleurs des textes
			ActivityMainBinding binding = getBinding();
			binding.mainActivityUsernameTxt.setTextColor(getColor(currentColors.get(R.color.text_primary)));
			binding.mainActivityPasswordTxt.setTextColor(getColor(currentColors.get(R.color.text_primary)));
			binding.mainActivityRgpdChx.setTextColor(getColor(currentColors.get(R.color.text_secondary)));

			// Mise à jour des arrière-plans
			binding.mainActivityConnectLyt.setBackgroundColor(getColor(currentColors.get(R.color.background_primary)));
			binding.mainActivityDecoLyt.setBackgroundColor(getColor(currentColors.get(R.color.background_primary)));
			binding.mainActivityVersionLyt.setBackgroundColor(getColor(currentColors.get(R.color.background_primary)));

			// Mise à jour des boutons
			updateButtonTheme(binding.mainActivityConnectBtn, currentColors);
			updateButtonTheme(binding.mainActivityDecoBtn, currentColors);
			updateButtonTheme(binding.mainActivityRobotBtn, currentColors);
			updateButtonTheme(binding.mainActivityMailBtn, currentColors);

			// Mise à jour des champs de saisie
			updateEditTextTheme(binding.mainActivityUsernameTxt, currentColors);
			updateEditTextTheme(binding.mainActivityPasswordTxt, currentColors);

			if (uiEventListener != null) {
				uiEventListener.onThemeChanged(isDarkMode);
			}
		}

		private void updateButtonTheme(View button, Map<Integer, Integer> colors) {
			int backgroundColor = getColor(colors.get(R.color.button_background));
			compatManager.applyBackgroundTint(button, backgroundColor);

			if (button instanceof Button) {
				((Button) button).setTextColor(getColor(colors.get(R.color.button_text)));
			}
		}

		private void updateEditTextTheme(EditText editText, Map<Integer, Integer> colors) {
			editText.setBackgroundTintList(ColorStateList.valueOf(getColor(colors.get(R.color.text_primary))));
			editText.setHintTextColor(getColor(colors.get(R.color.text_secondary)));
		}

		private int getColor(int colorRes) {
			return getActivity().getColor(colorRes);
		}

		boolean isDarkMode() {
			return isDarkMode;
		}
	}

	private void initializeTheme() {
		// Détecter le mode nuit système
		int currentNightMode = getActivity().getResources().getConfiguration().uiMode
				& Configuration.UI_MODE_NIGHT_MASK;
		boolean isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
		themeManager.updateTheme(isDarkMode);
	}

	/**
	 * Met à jour le thème de l'application
	 */
	public void updateTheme(boolean darkMode) {
		themeLock.writeLock().lock();
		try {
			themeManager.updateTheme(darkMode);
		} finally {
			themeLock.writeLock().unlock();
		}
	}

	/**
	 * Récupère les métriques UI actuelles
	 */
	@Override
	public Map<String, Object> getUIMetrics() {
		synchronized (metricsLock) {
			return new HashMap<>(uiMetrics.getMetrics());
		}
	}

	/**
	 * Réinitialise les métriques UI
	 */
	public void resetMetrics() {
		synchronized (metricsLock) {
			uiMetrics.reset();
		}
	}

	// Amélioration de la méthode dispose
	public void dispose() {
		try {
			themeLock.writeLock().lock();
			synchronized (layoutLock) {
				synchronized (stateLock) {
					synchronized (metricsLock) {
						// Arrêt des analytics
						analytics.endSession();

						// Suppression des callbacks
						uiEventListener = null;
						eventListeners.clear();
						errorHandler = null;

						// Nettoyage des handlers
						uiHandler.removeCallbacksAndMessages(null);

						// Sauvegarde des métriques finales
						logger.info("Métriques finales UI: {}", uiMetrics.getMetrics());

						// Journaliser les métriques de performance finales
						Map<String, Object> finalMetrics = performanceTracker.getPerformanceReport();
						logger.info("Métriques de performance finales: {}", finalMetrics);

						// Reset des états
						errorTracker.reset();
						uiMetrics.reset();

						// Nettoyage des transitions personnalisées
						customTransitions.clear();

						// Nettoyage des références faibles
						activityRef.clear();
						bindingRef.clear();

						setSwipeEnabled(false);
						gestureDetector = null;

						viewStateCache.clearCache();
					}
				}
			}
		} finally {
			themeLock.writeLock().unlock();
		}
	}

	private void setupAnalytics() {
		analytics.addListener(new UIAnalytics.AnalyticsEventListener() {
			@Override
			public void onEventTracked(UIAnalytics.AnalyticsEvent event) {
				logger.debug("Analytics event: {} - {}", event.type, event.action);
			}

			@Override
			public void onSessionEnded(UIAnalytics.AnalyticsSession session) {
				logger.info("Session analytics: {}", session.getSessionMetrics());
			}
		});

		// Démarrer une nouvelle session
		analytics.startSession("main_activity_" + System.currentTimeMillis());
	}

	private final Map<TransitionKey, LayoutTransition> customTransitions = new HashMap<>();

	private static class TransitionKey {
		final int fromLayout;
		final int toLayout;

		TransitionKey(int fromLayout, int toLayout) {
			this.fromLayout = fromLayout;
			this.toLayout = toLayout;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			TransitionKey that = (TransitionKey) o;
			return fromLayout == that.fromLayout && toLayout == that.toLayout;
		}

		@Override
		public int hashCode() {
			return Objects.hash(fromLayout, toLayout);
		}
	}

	public interface TransitionCallback {
		void onTransitionComplete();
	}

	/**
	 * Ajoute une transition personnalisée entre deux layouts
	 */
	public void setCustomTransition(@LayoutType int fromLayout,
			@LayoutType int toLayout,
			LayoutTransition transition) {
		customTransitions.put(new TransitionKey(fromLayout, toLayout), transition);
		logger.debug("Transition personnalisée ajoutée: {} -> {}", fromLayout, toLayout);
	}

	/**
	 * Crée une transition avec une animation personnalisée
	 */
	public void executeTransition(View showView, View hideView, @LayoutType int newLayout) {
		TransitionKey key = new TransitionKey(uiState.currentLayout, newLayout);
		LayoutTransition customTransition = customTransitions.get(key);

		if (customTransition != null) {
			compatManager.applyTransition(showView, customTransition.getAnimator());
		} else {
			animateLayoutTransition(showView, hideView, newLayout);
		}
	}

	/**
	 * Exemple de création d'une transition personnalisée
	 */
	public static LayoutTransition createSlideTransition(boolean slideLeft) {
		ObjectAnimator slideOut = ObjectAnimator.ofFloat(null, "translationX",
				0f, slideLeft ? -1000f : 1000f);
		ObjectAnimator slideIn = ObjectAnimator.ofFloat(null, "translationX",
				slideLeft ? 1000f : -1000f, 0f);

		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playSequentially(slideOut, slideIn);

		return new LayoutTransition(animatorSet, 500, 0, 0);
	}

	/**
	 * Exemple de création d'une transition avec fondu et échelle
	 */
	public static LayoutTransition createFadeScaleTransition() {
		AnimatorSet animatorSet = new AnimatorSet();

		ObjectAnimator fadeOut = ObjectAnimator.ofFloat(null, "alpha", 1f, 0f);
		ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(null, "scaleX", 1f, 0.3f);
		ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(null, "scaleY", 1f, 0.3f);

		ObjectAnimator fadeIn = ObjectAnimator.ofFloat(null, "alpha", 0f, 1f);
		ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(null, "scaleX", 0.3f, 1f);
		ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(null, "scaleY", 0.3f, 1f);

		AnimatorSet hideAnim = new AnimatorSet();
		hideAnim.playTogether(fadeOut, scaleDownX, scaleDownY);

		AnimatorSet showAnim = new AnimatorSet();
		showAnim.playTogether(fadeIn, scaleUpX, scaleUpY);

		animatorSet.playSequentially(hideAnim, showAnim);

		return new LayoutTransition(animatorSet, 400, 0, 0);
	}

	private UITestSupport testSupport;

	@VisibleForTesting
	public void enableTestSupport() {
		if (testSupport == null) {
			testSupport = new UITestSupport(this);
		}
	}

	@VisibleForTesting
	public UITestSupport getTestSupport() {
		if (testSupport == null) {
			throw new IllegalStateException("Test support non activé. Appelez enableTestSupport() d'abord.");
		}
		return testSupport;
	}

	@VisibleForTesting
	public void simulateUserInteraction(String actionType, Map<String, Object> params) {
		if (testSupport != null) {
			testSupport.simulateUserInteraction(actionType, params);
		}
	}

	// Gestion des opérations UI de manière sécurisée
	private void runOnUiThread(Runnable action) {
		try {
			if (Looper.myLooper() == Looper.getMainLooper()) {
				action.run();
			} else {
				uiHandler.post(() -> {
					try {
						validateContext();
						action.run();
					} catch (Exception e) {
						logger.error("Erreur lors de l'exécution sur le thread UI", e);
					}
				});
			}
		} catch (Exception e) {
			logger.error("Erreur lors de la planification sur le thread UI", e);
		}
	}

	// Protection de l'état UI
	private void updateUIState(@LayoutType int newLayout) {
		synchronized (stateLock) {
			uiState.setLayout(newLayout);
			uiState.lastStateChangeTime = System.currentTimeMillis();
		}
	}

	public void setupElevation(float elevation) {
		ActivityMainBinding binding = getBinding();
		View[] elevatedViews = {
				binding.mainActivityConnectLyt,
				binding.mainActivityDecoLyt,
				binding.mainActivityVersionLyt
		};

		for (View view : elevatedViews) {
			compatManager.applyElevation(view, elevation);
		}
	}

	private void setupGestureSupport() {
		gestureDetector = new GestureDetector(getActivity(),
				new SwipeGestureDetector(this));

		// Ajouter les listeners de touch sur les layouts
		ActivityMainBinding binding = getBinding();
		View[] layouts = {
				binding.mainActivityConnectLyt,
				binding.mainActivityDecoLyt,
				binding.mainActivityVersionLyt
		};

		for (View layout : layouts) {
			layout.setOnTouchListener((v, event) -> {
				if (swipeEnabled) {
					return gestureDetector.onTouchEvent(event);
				}
				return false;
			});
		}
	}

	public void setSwipeEnabled(boolean enabled) {
		this.swipeEnabled = enabled;
		logger.debug("Support des swipes {}", enabled ? "activé" : "désactivé");
	}

	@Override
	public void onSwipeRight() {
		synchronized (layoutLock) {
			try {
				switch (uiState.currentLayout) {
					case LAYOUT_CONNECT:
						showDisconnectLayout();
						break;
					case LAYOUT_DISCONNECT:
						showVersionLayout();
						break;
				}
				analytics.trackEvent(
						UIAnalytics.EventType.GESTURE,
						"swipe_right",
						Map.of("fromLayout", uiState.currentLayout));
			} catch (Exception e) {
				logger.error("Erreur lors du swipe droite", e);
			}
		}
	}

	@Override
	public void onSwipeLeft() {
		synchronized (layoutLock) {
			try {
				switch (uiState.currentLayout) {
					case LAYOUT_VERSION:
						showDisconnectLayout();
						break;
					case LAYOUT_DISCONNECT:
						showConnectLayout();
						break;
				}
				analytics.trackEvent(
						UIAnalytics.EventType.GESTURE,
						"swipe_left",
						Map.of("fromLayout", uiState.currentLayout));
			} catch (Exception e) {
				logger.error("Erreur lors du swipe gauche", e);
			}
		}
	}

	@Override
	public void onSwipeTop() {
		// Implémentation si nécessaire
		logger.debug("Swipe haut ignoré");
	}

	@Override
	public void onSwipeBottom() {
		// Implémentation si nécessaire
		logger.debug("Swipe bas ignoré");
	}

	/**
	 * Met à jour la configuration des gestes
	 */
	public void updateGestureConfiguration(Configuration newConfig) {
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// Désactiver les swipes en mode paysage
			setSwipeEnabled(false);
		} else {
			setSwipeEnabled(true);
		}
	}

	private void cacheCurrentLayout() {
		ActivityMainBinding binding = getBinding();
		viewStateCache.cacheViewState(R.id.mainActivityConnectLyt, binding.mainActivityConnectLyt);
		viewStateCache.cacheViewState(R.id.mainActivityDecoLyt, binding.mainActivityDecoLyt);
		viewStateCache.cacheViewState(R.id.mainActivityVersionLyt, binding.mainActivityVersionLyt);

		// Cache des éléments d'interface importants
		viewStateCache.cacheViewState(R.id.mainActivityUsernameTxt, binding.mainActivityUsernameTxt);
		viewStateCache.cacheViewState(R.id.mainActivityPasswordTxt, binding.mainActivityPasswordTxt);
		viewStateCache.cacheViewState(R.id.mainActivityRgpdChx, binding.mainActivityRgpdChx);
		viewStateCache.cacheViewState(R.id.mainActivityConnectBtn, binding.mainActivityConnectBtn);
	}

	private void restoreFromCache() {
		ActivityMainBinding binding = getBinding();
		viewStateCache.restoreViewState(R.id.mainActivityConnectLyt, binding.mainActivityConnectLyt);
		viewStateCache.restoreViewState(R.id.mainActivityDecoLyt, binding.mainActivityDecoLyt);
		viewStateCache.restoreViewState(R.id.mainActivityVersionLyt, binding.mainActivityVersionLyt);

		viewStateCache.restoreViewState(R.id.mainActivityUsernameTxt, binding.mainActivityUsernameTxt);
		viewStateCache.restoreViewState(R.id.mainActivityPasswordTxt, binding.mainActivityPasswordTxt);
		viewStateCache.restoreViewState(R.id.mainActivityRgpdChx, binding.mainActivityRgpdChx);
		viewStateCache.restoreViewState(R.id.mainActivityConnectBtn, binding.mainActivityConnectBtn);
	}

	private void setupAccessibility() {
		ActivityMainBinding binding = getBinding();

		// Configuration des champs de saisie
		accessibilityManager.setupEditTextAccessibility(
				binding.mainActivityUsernameTxt,
				"Nom d'utilisateur",
				"Saisissez votre nom d'utilisateur");

		accessibilityManager.setupEditTextAccessibility(
				binding.mainActivityPasswordTxt,
				"Mot de passe",
				"Saisissez votre mot de passe");

		// Configuration des boutons
		accessibilityManager.setupButtonAccessibility(
				binding.mainActivityConnectBtn,
				"Se connecter",
				"Lance la connexion à l'application");

		accessibilityManager.setupButtonAccessibility(
				binding.mainActivityDecoBtn,
				"Se déconnecter",
				"Déconnecte l'utilisateur de l'application");

		// Configuration de la case à cocher RGPD
		accessibilityManager.setupAccessibility(
				binding.mainActivityRgpdChx,
				"Acceptation RGPD",
				"Cochez pour accepter les conditions RGPD");
	}

	public void setAccessibilityEnabled(boolean enabled) {
		accessibilityManager.setAccessibilityEnabled(enabled);
	}

	/**
	 * Récupère le rapport de performance UI
	 */
	public Map<String, Object> getPerformanceReport() {
		return performanceTracker.getPerformanceReport();
	}
}
