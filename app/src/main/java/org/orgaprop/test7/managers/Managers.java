package org.orgaprop.test7.managers;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import android.content.Intent;

import org.orgaprop.test7.databinding.ActivitySelectBinding;
import org.orgaprop.test7.handlers.ActivityResultHandler;
import org.orgaprop.test7.handlers.ErrorHandler;
import org.orgaprop.test7.models.GlobalState;
import org.orgaprop.test7.models.SelectionState;
import org.orgaprop.test7.viewmodels.SelectActivityViewModel;

/**
 * Gestionnaire centralisé des différents managers de l'application
 */
public class Managers {

	private final ActivityStateManager stateManager;
	private final SearchManager searchManager;
	private final KeyboardManager keyboardManager;
	private final ValidationManager validationManager;
	private final ActivityResultHandler resultHandler;
	private final ErrorHandler errorHandler;
	private final SelectionPrefsManager prefsManager;

	public Managers(Activity activity, ActivitySelectBinding binding, SelectActivityViewModel viewModel) {
		GlobalState globalState = GlobalState.getInstance();
		SelectionState selectionState = new SelectionState();

		this.stateManager = new ActivityStateManager();
		this.keyboardManager = new KeyboardManager(activity);
		this.searchManager = new SearchManager(activity, viewModel, stateManager);
		this.validationManager = new ValidationManager(activity, selectionState);
		this.errorHandler = new ErrorHandler(activity, stateManager);
		this.resultHandler = new ActivityResultHandler(selectionState, globalState, binding);
		this.prefsManager = new SelectionPrefsManager(activity);
	}

	public ActivityStateManager getStateManager() {
		return stateManager;
	}

	public SearchManager getSearchManager() {
		return searchManager;
	}

	public KeyboardManager getKeyboardManager() {
		return keyboardManager;
	}

	public ValidationManager getValidationManager() {
		return validationManager;
	}

	public ActivityResultHandler getResultHandler() {
		return resultHandler;
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public SelectionPrefsManager getPrefsManager() {
		return prefsManager;
	}

	public void dispose() {
		searchManager.dispose();
	}

}
