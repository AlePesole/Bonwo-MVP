package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidEmailException extends DomainException {
    public InvalidEmailException(String email) { super("Invalid email address: " + email); }
}
