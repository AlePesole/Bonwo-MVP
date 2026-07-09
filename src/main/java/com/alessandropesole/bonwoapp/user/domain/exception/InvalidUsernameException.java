package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidUsernameException extends DomainException {
    public InvalidUsernameException() {
        super("Username must be 3-30 characters and contain only letters, numbers and underscores");
    }
}
