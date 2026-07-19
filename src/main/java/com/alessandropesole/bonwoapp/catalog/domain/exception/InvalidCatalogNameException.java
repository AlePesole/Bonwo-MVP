package com.alessandropesole.bonwoapp.catalog.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidCatalogNameException extends DomainException {
    public InvalidCatalogNameException(String message) { super(message); }
}
