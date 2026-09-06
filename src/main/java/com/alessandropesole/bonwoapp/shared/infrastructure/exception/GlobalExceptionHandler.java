package com.alessandropesole.bonwoapp.shared.infrastructure.exception;

import com.alessandropesole.bonwoapp.catalog.application.exception.DuplicateNameException;
import com.alessandropesole.bonwoapp.media.domain.exception.MediaNotOwnedException;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyCompletedException;
import com.alessandropesole.bonwoapp.session.domain.exception.SessionAlreadyInProgressException;
import com.alessandropesole.bonwoapp.shared.domain.DomainException;
import com.alessandropesole.bonwoapp.user.application.exception.EmailAlreadyRegisteredException;
import com.alessandropesole.bonwoapp.user.application.exception.InvalidRefreshTokenException;
import com.alessandropesole.bonwoapp.user.application.exception.UsernameAlreadyTakenException;
import com.alessandropesole.bonwoapp.user.domain.exception.AlreadyBannedException;
import com.alessandropesole.bonwoapp.user.domain.exception.NotBannedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        detail.setType(URI.create("/errors/unauthorized"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler({
            EmailAlreadyRegisteredException.class,
            UsernameAlreadyTakenException.class,
            DuplicateNameException.class,
            SessionAlreadyInProgressException.class,
            SessionAlreadyCompletedException.class,
            AlreadyBannedException.class,
            NotBannedException.class
    })
    public ProblemDetail handleConflict(DomainException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setType(URI.create("/errors/conflict"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This action conflicts with existing data");
        detail.setType(URI.create("/errors/conflict"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setType(URI.create("/errors/bad-request"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'");
        detail.setType(URI.create("/errors/bad-request"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
        detail.setType(URI.create("/errors/bad-request"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setType(URI.create("/errors/bad-request"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        detail.setType(URI.create("/errors/domain-rule-violation"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setType(URI.create("/errors/not-found"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No endpoint found for this request");
        detail.setType(URI.create("/errors/not-found"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Your account has been banned");
        detail.setType(URI.create("/errors/forbidden"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        detail.setType(URI.create("/errors/unauthorized"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        detail.setType(URI.create("/errors/forbidden"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler({ForbiddenOperationException.class, MediaNotOwnedException.class})
    public ProblemDetail handleForbiddenOperation(RuntimeException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setType(URI.create("/errors/forbidden"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
        detail.setType(URI.create("/errors/method-not-allowed"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        detail.setType(URI.create("/errors/unsupported-media-type"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "The uploaded file exceeds the maximum allowed size");
        detail.setType(URI.create("/errors/payload-too-large"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a
                ));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        detail.setType(URI.create("/errors/validation"));
        detail.setProperty("errors", errors);
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        detail.setType(URI.create("/errors/internal"));
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
