package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class AlreadyBannedException extends DomainException {
    public AlreadyBannedException(String user) { super("User is already banned: " + user); }
}
