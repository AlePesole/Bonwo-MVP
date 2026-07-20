package com.alessandropesole.bonwoapp.media.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class MediaUploadFailedException extends DomainException {
    public MediaUploadFailedException(String reason) {
        super("Media upload failed: " + reason);
    }
}
