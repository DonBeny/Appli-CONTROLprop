package org.orgaprop.test7.managers;

public class ActivityStateManager {

	private boolean isStarted;
	private boolean canCheck;
	private boolean waitDownload;

	public void reset() {
		isStarted = false;
		canCheck = true;
		waitDownload = false;
	}

	public boolean canStartActivity() {
		return !isStarted && canCheck && !waitDownload;
	}

	public void markActivityStarted() {
		isStarted = true;
		canCheck = false;
	}

	public void onResume() {
		if (canCheck) {
			isStarted = false;
		}

		canCheck = true;
		waitDownload = false;
	}

	public void setWaitDownload(boolean wait) {
		this.waitDownload = wait;
	}

	public boolean isWaitingDownload() {
		return waitDownload;
	}

}
