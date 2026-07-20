package com.alessandropesole.bonwoapp.shared.infrastructure.exception;

public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
