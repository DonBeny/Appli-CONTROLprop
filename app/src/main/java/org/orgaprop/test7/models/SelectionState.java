package org.orgaprop.test7.models;

public class SelectionState {
	public enum State {
		IDLE,
		SELECTING_AGENCE,
		SELECTING_GROUPEMENT,
		SELECTING_RESIDENCE,
		COMPLETE,
		ERROR
	}

	private int agenceId = -1;
	private int groupementId = -1;
	private int residenceId = -1;
	private State currentState = State.IDLE;
	private String errorMessage;

	public boolean isComplete() {
		return currentState == State.COMPLETE;
	}

	public void reset() {
		agenceId = -1;
		groupementId = -1;
		residenceId = -1;
		currentState = State.IDLE;
		errorMessage = null;
	}

	public void setAgence(int id) {
		if (agenceId != id) {
			agenceId = id;
			groupementId = -1;
			residenceId = -1;
			currentState = State.SELECTING_GROUPEMENT;
		}
	}

	public void setGroupement(int id) {
		if (groupementId != id) {
			groupementId = id;
			residenceId = -1;
			currentState = State.SELECTING_RESIDENCE;
		}
	}

	public void setResidence(int id) {
		residenceId = id;
		currentState = State.COMPLETE;
	}

	public int getAgenceId() {
		return agenceId;
	}

	public int getGroupementId() {
		return groupementId;
	}

	public int getResidenceId() {
		return residenceId;
	}

	public State getCurrentState() {
		return currentState;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setError(String message) {
		currentState = State.ERROR;
		errorMessage = message;
	}

	@Override
	public String toString() {
		return String.format("SelectionState{state=%s, agence=%d, groupement=%d, residence=%d}",
				currentState, agenceId, groupementId, residenceId);
	}
}
