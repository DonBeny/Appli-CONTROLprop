package org.orgaprop.test7.managers;

import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;

import org.orgaprop.test7.models.UiState;
import org.orgaprop.test7.utils.IntentFactory;

public class ActivityManager {

	private final Activity activity;
	private final UiState uiState;
	private final ActivityResultLauncher<Intent> launcher;

	public ActivityManager(Activity activity, UiState uiState, ActivityResultLauncher<Intent> launcher) {
		this.activity = activity;
		this.uiState = uiState;
		this.launcher = launcher;
	}

	public void launchSearch(String searchText) {
		if (searchText.isEmpty())
			return;

		uiState.markActivityStarted();
		launcher.launch(IntentFactory.createSearchIntent(activity, searchText));
	}

	public void launchSelection(String type, int parentId, boolean useCache) {
		Intent intent = useCache
				? IntentFactory.createSelectIntent(activity, type, cachedLists.get(type))
				: IntentFactory.createSelectWithParentIntent(activity, type, parentId);

		launcher.launch(intent);
	}

	public void launchControl(SelectItem ficheResid, boolean isProxi, boolean isContra) {
		if (!uiState.canStartActivity())
			return;

		uiState.markActivityStarted();
		Intent intent = IntentFactory.createTypeCtrlIntent(activity, ficheResid, isProxi, isContra);
		activity.startActivity(intent);
	}

}
