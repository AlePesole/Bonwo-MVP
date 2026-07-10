package com.alessandropesole.bonwoapp.user.application.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String email) { super("User not found with email: " +  email); }
}
