package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidProfileDataException extends DomainException {
    public InvalidProfileDataException(String msg) { super(msg); }
}
