package com.alessandropesole.bonwoapp.exercise.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidMuscleEntryException extends DomainException {
    public InvalidMuscleEntryException(String msg) {
        super(msg);
    }
}
