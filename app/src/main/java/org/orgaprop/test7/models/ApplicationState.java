package org.orgaprop.test7.models;

public class ApplicationState {

	private final UiState uiState;
	private final SelectionState selectionState;
	private final Prefs prefs;

	public ApplicationState(Context context) {
		this.uiState = new UiState();
		this.selectionState = new SelectionState();
		this.prefs = new Prefs(context);
	}

	public void reset() {
		uiState.reset();
		selectionState.reset();
	}

	public UiState getUiState() {
		return uiState;
	}

	public SelectionState getSelectionState() {
		return selectionState;
	}

	public Prefs getPrefs() {
		return prefs;
	}

}
