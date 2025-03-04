package org.orgaprop.test7.models;

public class UiState {

	private boolean isStarted;
	private boolean canCheck;
	private boolean waitDownload;

	public UiState() {
		reset();
	}

	public void reset() {
		isStarted = false;
		canCheck = true;
		waitDownload = false;
	}

	public boolean canStartActivity() {
		return !isStarted;
	}

	public void markActivityStarted() {
		isStarted = true;
		canCheck = false;
	}

	public void setWaitDownload(boolean wait) {
		waitDownload = wait;
	}

	public boolean isWaitingDownload() {
		return waitDownload;
	}

	public boolean isCheckEnabled() {
		return canCheck;
	}

	public void enableCheck() {
		canCheck = true;
	}

}
