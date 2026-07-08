package com.alessandropesole.bonwoapp.catalog.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidEquipmentNameException extends DomainException {
    public InvalidEquipmentNameException(String message) {
        super(message);
    }
}
