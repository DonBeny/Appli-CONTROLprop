package org.orgaprop.test7.models;

public enum SelectionType {

	AGENCE("agc"),
	GROUPEMENT("grp"),
	RESIDENCE("rsd"),
	SEARCH("search");

	private final String code;

	SelectionType(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static SelectionType fromCode(String code) {
		for (SelectionType type : values()) {
			if (type.code.equals(code)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Code invalide : " + code);
	}

}
