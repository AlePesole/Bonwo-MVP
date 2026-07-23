package com.alessandropesole.bonwoapp.routine.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidSetConfigException extends DomainException {
    public InvalidSetConfigException(String msg) { super(msg); }
}
