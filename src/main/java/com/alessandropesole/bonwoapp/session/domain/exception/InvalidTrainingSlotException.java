package com.alessandropesole.bonwoapp.session.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidTrainingSlotException extends DomainException {
    public InvalidTrainingSlotException(String message) {
        super(message);
    }
}
