package com.alessandropesole.bonwoapp.exercise.domain.exception.publication;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidExercisePublicationException extends DomainException {
    public InvalidExercisePublicationException(String message) {
        super(message);
    }
}
