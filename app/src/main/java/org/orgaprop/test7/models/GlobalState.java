package org.orgaprop.test7.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalState {

	private static GlobalState instance;
	private final Map<String, List<?>> cachedLists = new HashMap<>();
	private SelectItem ficheResid;

	private GlobalState() {
	}

	public static GlobalState getInstance() {
		if (instance == null) {
			instance = new GlobalState();
		}

		return instance;
	}

	public Map<String, List<?>> getCachedLists() {
		return cachedLists;
	}

	public SelectItem getFicheResid() {
		return ficheResid;
	}

	public void setFicheResid(SelectItem ficheResid) {
		this.ficheResid = ficheResid;
	}

}
