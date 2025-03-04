package org.orgaprop.test7.security.validation;

import androidx.annotation.NonNull;
import org.orgaprop.test7.exceptions.auth.core.AuthenticationException;

public interface ICredentialsValidator {
	void validateCredentials(@NonNull String username, @NonNull String password)
			throws AuthenticationException;

	boolean isValid(@NonNull String username, @NonNull String password);
}
