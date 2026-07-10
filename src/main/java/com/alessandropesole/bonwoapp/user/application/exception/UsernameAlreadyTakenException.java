package com.alessandropesole.bonwoapp.user.application.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class UsernameAlreadyTakenException extends DomainException {
    public UsernameAlreadyTakenException(String username) { super("Username is already taken: " + username); }
}
