package com.alessandropesole.bonwoapp.program.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class InvalidTrainingProgramException extends DomainException {
    public InvalidTrainingProgramException(String msg) { super(msg); }
}
