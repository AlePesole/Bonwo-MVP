package com.alessandropesole.bonwoapp.shared.infrastructure.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }

    /** Constructor for multi-id validation failures. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
