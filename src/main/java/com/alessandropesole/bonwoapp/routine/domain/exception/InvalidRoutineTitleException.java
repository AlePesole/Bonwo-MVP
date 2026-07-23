package com.alessandropesole.bonwoapp.routine.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidRoutineTitleException extends DomainException {
    public InvalidRoutineTitleException(String message) {
        super(message);
    }
}
