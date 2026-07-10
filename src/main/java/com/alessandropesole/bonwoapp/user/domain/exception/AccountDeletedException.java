package com.alessandropesole.bonwoapp.user.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class AccountDeletedException extends DomainException {
    public AccountDeletedException(String user) { super("This account has been deleted: " + user); }
}
