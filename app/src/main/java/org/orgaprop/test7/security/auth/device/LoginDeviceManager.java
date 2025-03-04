package org.orgaprop.test7.security.auth.device;

import android.content.Context;
import android.os.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginDeviceManager {
	private static final Logger logger = LoggerFactory.getLogger(LoginDeviceManager.class);
	private final Context context;

	private String currentDeviceId;

	public LoginDeviceManager(Context context) {
		this.context = context;
		this.currentDeviceId = Build.FINGERPRINT;
	}

	public boolean validateDeviceId(String serverDeviceId) {
		if (!currentDeviceId.equals(serverDeviceId)) {
			logger.warn("Device ID non reconnu - Serveur: {}, Local: {}",
					serverDeviceId, currentDeviceId);
			return false;
		}
		return true;
	}

	public String getCurrentDeviceId() {
		return currentDeviceId;
	}

	public void updateDeviceId() {
		this.currentDeviceId = Build.FINGERPRINT;
		logger.debug("Device ID mis à jour: {}", currentDeviceId);
	}
}
