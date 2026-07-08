package com.alessandropesole.bonwoapp.media.domain.exception;

import com.alessandropesole.bonwoapp.shared.domain.DomainException;

public class VideoNotReadyException extends DomainException {
    public VideoNotReadyException(Long videoId) {
        super("Video " + videoId + " is not ready yet - wait for processing to complete");
    }
}
