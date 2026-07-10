package com.alessandropesole.bonwoapp.user.application.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class EmailAlreadyRegisteredException extends DomainException {
    public EmailAlreadyRegisteredException(String email) { super("Email is already registered: " + email); }
}
