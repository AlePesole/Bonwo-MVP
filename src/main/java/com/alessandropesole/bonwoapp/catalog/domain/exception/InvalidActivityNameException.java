package com.alessandropesole.bonwoapp.catalog.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidActivityNameException extends DomainException {
    public InvalidActivityNameException(String message) {
        super(message);
    }
}
