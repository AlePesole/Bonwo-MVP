package com.alessandropesole.bonwoapp.routine.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidSlotException extends DomainException {
    public InvalidSlotException(String msg) { super(msg); }
}
