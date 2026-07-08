package com.alessandropesole.bonwoapp.media.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class MediaNotOwnedException extends DomainException {
    public MediaNotOwnedException() {
        super("You do not own this media resource");
    }
}
