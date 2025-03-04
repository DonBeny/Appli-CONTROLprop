package org.orgaprop.test7.security.diagnostic;

public enum NotificationStatus {
	PENDING("EN_ATTENTE"),
	DELIVERED("LIVRÉ"),
	FAILED("ÉCHOUÉ"),
	RETRY("NOUVELLE_TENTATIVE");

	private final String label;

	NotificationStatus(String label) {
		this.label = label;
	}

	// Ajout du getter pour accéder au label
	public String getLabel() {
		return label;
	}
}
