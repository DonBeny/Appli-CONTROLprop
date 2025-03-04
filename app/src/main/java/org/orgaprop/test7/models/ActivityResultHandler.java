package org.orgaprop.test7.models;

import android.content.Intent;
import androidx.annotation.StringRes;
import org.json.JSONException;
import org.json.JSONObject;
import org.orgaprop.test7.R;
import org.orgaprop.test7.constants.IntentKeys;
import org.orgaprop.test7.databinding.ActivitySelectBinding;
import org.orgaprop.test7.utils.UiUtils;

import java.util.List;
import java.util.Objects;

public class ActivityResultHandler {
	private final SelectionState selectionState;
	private final GlobalState globalState;
	private final ActivitySelectBinding binding;
	private final ErrorHandler errorHandler;

	public ActivityResultHandler(SelectionState selectionState,
			GlobalState globalState,
			ActivitySelectBinding binding,
			ErrorHandler errorHandler) {
		this.selectionState = selectionState;
		this.globalState = globalState;
		this.binding = binding;
		this.errorHandler = errorHandler;
	}

	public void handleSearchResult(Intent data) {
		try {
			JSONObject jsonComment = new JSONObject(Objects.requireNonNull(
					data.getStringExtra(IntentKeys.Search.COMMENT)));
			int rsdId = data.getIntExtra(IntentKeys.Search.ID, 0);

			if (rsdId > 0) {
				updateFromSearchResult(jsonComment, rsdId,
						data.getStringExtra(IntentKeys.Search.TEXT));
			}
		} catch (JSONException e) {
			errorHandler.handleError(e, R.string.error_processing_data);
		}
	}

	public void handleListResult(String type, Intent data) {
		try {
			cacheReceivedList(type, data);
			updateSelectionFromType(type, data);
		} catch (Exception e) {
			errorHandler.handleError(e, R.string.error_processing_result);
		}
	}

	private void updateFromSearchResult(JSONObject jsonComment, int rsdId, String rsdText)
			throws JSONException {
		JSONObject agency = jsonComment.getJSONObject("agency");
		JSONObject groupe = jsonComment.getJSONObject("groupe");

		selectionState.setAgence(agency.getInt("id"));
		selectionState.setGroupement(groupe.getInt("id"));
		selectionState.setResidence(rsdId);

		binding.selectActivityAgcSpinner.setText(agency.getString("txt"));
		binding.selectActivityGrpSpinner.setText(groupe.getString("txt"));
		binding.selectActivityRsdSpinner.setText(rsdText);
	}

	private void cacheReceivedList(String type, Intent data) {
		List<?> receivedList = (List<?>) data.getSerializableExtra(IntentKeys.Search.LIST);
		if (receivedList != null) {
			globalState.getCachedLists().put(type, receivedList);
		}
	}

	private void updateSelectionFromType(String type, Intent data) {
		int selectedId = data.getIntExtra(IntentKeys.Search.ID, 0);
		if (selectedId <= 0)
			return;

		String selectedText = data.getStringExtra(IntentKeys.Search.TEXT);

		switch (type) {
			case SelectListActivity.SELECT_LIST_TYPE_AGC:
				updateAgenceSelection(selectedId, selectedText);
				break;
			case SelectListActivity.SELECT_LIST_TYPE_GRP:
				updateGroupementSelection(selectedId, selectedText);
				break;
			case SelectListActivity.SELECT_LIST_TYPE_RSD:
				updateResidenceSelection(selectedId, selectedText, data);
				break;
		}
	}

	private void updateAgenceSelection(int id, String text) {
		selectionState.setAgence(id);
		binding.selectActivityAgcSpinner.setText(text);
		resetDependentSpinners();
	}

	private void updateGroupementSelection(int id, String text) {
		selectionState.setGroupement(id);
		binding.selectActivityGrpSpinner.setText(text);
		binding.selectActivityRsdSpinner.setText(R.string.select_residence);
	}

	private void updateResidenceSelection(int id, String text, Intent data) {
		selectionState.setResidence(id);
		binding.selectActivityRsdSpinner.setText(text);
		globalState.setFicheResid(
				(SelectItem) data.getSerializableExtra(IntentKeys.Search.COMMENT));
	}

	private void resetDependentSpinners() {
		binding.selectActivityGrpSpinner.setText(R.string.select_groupement);
		binding.selectActivityRsdSpinner.setText(R.string.select_residence);
	}
}
