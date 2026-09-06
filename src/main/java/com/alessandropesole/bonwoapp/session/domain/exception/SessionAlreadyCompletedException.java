package com.alessandropesole.bonwoapp.session.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class SessionAlreadyCompletedException extends DomainException {
    public SessionAlreadyCompletedException(String message) {
        super(message);
    }
}
