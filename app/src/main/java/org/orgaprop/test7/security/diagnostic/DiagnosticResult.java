package org.orgaprop.test7.security.diagnostic;

import lombok.Value;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Résultat d'un diagnostic de sécurité.
 * Cette classe est immuable.
 */
@Value
public final class DiagnosticResult {
	private final boolean isSecure;
	private final List<SecurityIssue> issues;

	public DiagnosticResult(boolean isSecure, List<SecurityIssue> issues) {
		this.isSecure = isSecure;
		this.issues = issues != null ? Collections.unmodifiableList(issues) : Collections.emptyList();
	}

	public boolean isSecure() {
		return isSecure;
	}

	public List<SecurityIssue> getIssues() {
		return issues;
	}
}
