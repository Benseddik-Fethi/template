package com.benseddik.template.error.record;
import org.springframework.http.HttpStatus;

/**
 * Exception applicative qui transporte directement un ErrorResponse.
 * Utile si tu veux lever une erreur métier "contrôlée" dans un service.
 */
public class ExceptionWithErrorResponse extends RuntimeException {
    private final ErrorResponse body;
    private final HttpStatus status;

    public ExceptionWithErrorResponse(HttpStatus status, ErrorResponse body) {
        super(body != null ? body.detail() : status.getReasonPhrase());
        this.status = status;
        this.body = body;
    }

    public ErrorResponse getBody() {
        return body;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
