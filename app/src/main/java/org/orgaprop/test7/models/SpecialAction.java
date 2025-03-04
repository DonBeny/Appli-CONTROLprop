package org.orgaprop.test7.models;

public enum SpecialAction {

	GO("go"),
	OFF("off"),
	CANCEL("cancel");

	private final String code;

	SpecialAction(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static SpecialAction fromCode(String code) {
		for (SpecialAction action : values()) {
			if (action.code.equals(code)) {
				return action;
			}
		}
		throw new IllegalArgumentException("Action invalide : " + code);
	}

}
