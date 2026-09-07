package com.alessandropesole.bonwoapp.shared.infrastructure.exception;

import com.alessandropesole.bonwoapp.media.domain.exception.MediaNotOwnedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleForbiddenOperation_mapsForbiddenOperationExceptionTo403() {
        var detail = handler.handleForbiddenOperation(new ForbiddenOperationException("You don't own this exercise"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(detail.getDetail()).isEqualTo("You don't own this exercise");
    }

    @Test
    void handleForbiddenOperation_mapsMediaNotOwnedExceptionTo403() {
        var detail = handler.handleForbiddenOperation(new MediaNotOwnedException());

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(detail.getDetail()).isEqualTo("You do not own this media resource");
    }
}
