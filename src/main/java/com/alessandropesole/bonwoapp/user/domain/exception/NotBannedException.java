package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class NotBannedException extends DomainException {
    public NotBannedException(String user) { super("User is not banned: " + user); }
}
