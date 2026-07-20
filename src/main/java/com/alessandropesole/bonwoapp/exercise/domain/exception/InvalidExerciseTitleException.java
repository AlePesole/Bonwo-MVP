package com.alessandropesole.bonwoapp.exercise.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidExerciseTitleException extends DomainException {
    public InvalidExerciseTitleException(String message) {
        super(message);
    }
}
