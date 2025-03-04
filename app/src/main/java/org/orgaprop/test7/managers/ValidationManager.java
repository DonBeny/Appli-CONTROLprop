package org.orgaprop.test7.managers;

import android.content.Context;
import android.widget.Toast;

import org.orgaprop.test7.models.SelectionState;

public class ValidationManager {

	private final Context context;
	private final SelectionState selectionState;

	public ValidationManager(Context context, SelectionState selectionState) {
		this.context = context;
		this.selectionState = selectionState;
	}

	public boolean validateParentSelection(String type, int parentId) {
		if (parentId < 0) {
			showValidationError(type);
			return false;
		}

		return true;
	}

	public boolean validateControlStart(boolean hasSelectedType) {
		return hasSelectedType && selectionState.isComplete();
	}

	private void showValidationError(String type) {
		String message = type.equals(SelectListActivity.SELECT_LIST_TYPE_GRP)
				? "Veuillez sélectionner une agence"
				: "Veuillez sélectionner un groupement";

		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

}
