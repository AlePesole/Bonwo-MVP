package com.alessandropesole.bonwoapp.session.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidTrainingSetException extends DomainException {
    public InvalidTrainingSetException(String message) {
        super(message);
    }
}
