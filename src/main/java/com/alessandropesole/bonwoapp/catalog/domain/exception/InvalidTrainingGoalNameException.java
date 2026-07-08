package com.alessandropesole.bonwoapp.catalog.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidTrainingGoalNameException extends DomainException {
    public InvalidTrainingGoalNameException(String message) {
        super(message);
    }
}
