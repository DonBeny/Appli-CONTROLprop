package org.orgaprop.test7.callbacks;

public interface SelectActivityCallbacks {

	void onSearchComplete(String result);

	void onSelectionComplete(SelectItem item);

	void onError(@StringRes int messageId);

	void onWaitingStateChanged(boolean isWaiting);

}
