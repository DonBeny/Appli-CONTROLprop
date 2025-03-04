package org.orgaprop.test7.managers;

import org.orgaprop.test7.models.SelectionState;
import org.orgaprop.test7.services.Prefs;

public class SelectionPrefsManager {

	private final Prefs prefs;

	public SelectionPrefsManager(Prefs prefs) {
		this.prefs = prefs;
	}

	public void saveSelection(SelectionState selection) {
		prefs.setAgency(String.valueOf(selection.getAgenceId()));
		prefs.setGroup(String.valueOf(selection.getGroupementId()));
		prefs.setResidence(String.valueOf(selection.getResidenceId()));
	}

	public void clearSelection() {
		prefs.setAgency("");
		prefs.setGroup("");
		prefs.setResidence("");
	}

}
