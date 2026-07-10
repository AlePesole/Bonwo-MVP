package com.alessandropesole.bonwoapp.user.application.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidRefreshTokenException extends DomainException {
    public InvalidRefreshTokenException() { super("Invalid or expired refresh token"); }
}
