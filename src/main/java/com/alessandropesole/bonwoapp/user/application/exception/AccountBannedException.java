package com.alessandropesole.bonwoapp.user.application.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class AccountBannedException extends DomainException {
    public AccountBannedException() { super("Your account has been banned"); }
}
