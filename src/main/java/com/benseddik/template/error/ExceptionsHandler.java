package com.benseddik.template.error;

import com.benseddik.template.error.record.ErrorResponse;
import com.benseddik.template.error.record.ExceptionWithErrorResponse;
import com.benseddik.template.error.record.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionsHandler {

    /* ========= Helpers ========= */

    private String path(HttpServletRequest req) {
        return req != null ? req.getRequestURI() : null;
    }

    private String correlationId() {
        return UUID.randomUUID().toString();
    }

    private ResponseEntity<ErrorResponse> respond(
            HttpStatus status, String title, String detail, String path, List<FieldError> errors
    ) {
        var body = ErrorResponse.of(status.value(), title, detail, path, correlationId(), errors);
        return ResponseEntity.status(status).body(body);
    }

    /* ========= Exceptions métier contrôlées ========= */

    @ExceptionHandler(ExceptionWithErrorResponse.class)
    public ResponseEntity<ErrorResponse> handleCustom(ExceptionWithErrorResponse ex, HttpServletRequest req) {
        var b = ex.getBody();
        // si la route n’a pas été fixée dans le body, on la renseigne
        if (b != null && (b.path() == null || b.path().isBlank())) {
            var enriched = ErrorResponse.of(
                    b.status(), b.title(), b.detail(), path(req), b.correlationId(), b.errors()
            );
            return ResponseEntity.status(ex.getStatus()).body(enriched);
        }
        return ResponseEntity.status(ex.getStatus()).body(b);
    }

    /* ========= Validation @RequestBody (Bean Validation) ========= */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String objectName = ex.getBindingResult().getObjectName(); // ex: "walkCreateRequest"
        List<FieldError> list = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(fe -> list.add(new FieldError(
                objectName,
                fe.getField(),
                fe.getDefaultMessage(),
                fe.getCode()
        )));

        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            list.add(new FieldError(
                    objectName,
                    null, // pas un champ spécifique
                    oe.getDefaultMessage(),
                    oe.getCode()
            ));
        }

        return respond(HttpStatus.BAD_REQUEST, "Bad Request", "Validation failed", path(req), list);
    }

    /* ========= Validation @RequestParam / @PathVariable ========= */

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldError> list = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : null;
            String code = Optional.ofNullable(v.getConstraintDescriptor())
                    .map(d -> d.getAnnotation().annotationType().getSimpleName())
                    .orElse("ConstraintViolation");
            list.add(new FieldError(
                    v.getRootBeanClass() != null ? v.getRootBeanClass().getSimpleName() : null,
                    field,
                    v.getMessage(),
                    code
            ));
        }
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", "Constraint violation", path(req), list);
    }

    /* ========= Corps JSON illisible / dates / enums / etc. ========= */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String shortMsg = Optional.of(ex.getMostSpecificCause())
                .map(Throwable::getMessage)
                .map(m -> m.length() > 300 ? m.substring(0, 300) + "…" : m)
                .orElse("Request body is not readable.");
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", shortMsg, path(req), List.of());
    }

    /* ========= Erreurs classiques HTTP ========= */

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        var fe = new FieldError(null, ex.getParameterName(), "required parameter is missing", "MissingServletRequestParameter");
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", "Missing parameter", path(req), List.of(fe));
    }

    @ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConversionFailedException.class })
    public ResponseEntity<ErrorResponse> handleTypeMismatch(Exception ex, HttpServletRequest req) {
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid parameter type", path(req), List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", ex.getMessage(), path(req), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage(), path(req), List.of());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest req) {
        List<FieldError> list = ex.getFieldErrors().stream()
                .map(fe -> new FieldError(
                        ex.getObjectName(),
                        fe.getField(),
                        fe.getDefaultMessage(),
                        fe.getCode()
                ))
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "Bad Request", "Binding failed", path(req), list);
    }

    /* ========= Sécurité ========= */

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return respond(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required", path(req), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccess(AccessDeniedException ex, HttpServletRequest req) {
        return respond(HttpStatus.FORBIDDEN, "Forbidden", "Access denied", path(req), List.of());
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex, HttpServletRequest req) {
        log.warn("JWT exception: {}", ex.getMessage());

        String detail;
        if (ex.getMessage().contains("expired") || ex.getMessage().contains("Expired")) {
            detail = "Token has expired";
        } else if (ex.getMessage().contains("invalid") || ex.getMessage().contains("Invalid")) {
            detail = "Invalid token";
        } else if (ex.getMessage().contains("Malformed")) {
            detail = "Malformed token";
        } else {
            detail = "JWT validation failed";
        }

        return respond(HttpStatus.UNAUTHORIZED, "Unauthorized", detail, path(req), List.of());
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex,
            HttpServletRequest req) {
        log.warn("Bad credentials attempt");
        return respond(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials", path(req), List.of());
    }

    /* ========= Fallback ========= */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", path(req), List.of());
    }

    


}