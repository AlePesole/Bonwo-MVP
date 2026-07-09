package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidUsernameException extends DomainException {
    public InvalidUsernameException(String message) {
        super(message);
    }
}
