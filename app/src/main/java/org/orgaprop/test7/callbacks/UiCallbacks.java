package org.orgaprop.test7.callbacks;

public interface UiCallbacks {

	void onLoadingStateChanged(boolean isLoading);

	void onError(@StringRes int messageId);

	void onSelectionUpdated(SelectionState state);

}
