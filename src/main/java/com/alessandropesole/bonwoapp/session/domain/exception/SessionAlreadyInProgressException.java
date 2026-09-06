package com.alessandropesole.bonwoapp.session.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class SessionAlreadyInProgressException extends DomainException {
    public SessionAlreadyInProgressException(String message) {
        super(message);
    }
}
