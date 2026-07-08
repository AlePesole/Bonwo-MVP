package com.alessandropesole.bonwoapp.catalog.application.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class DuplicateNameException extends DomainException {
    public DuplicateNameException(String entity, String name) {
        super(entity + " with name '" + name + "' already exists");
    }
}
