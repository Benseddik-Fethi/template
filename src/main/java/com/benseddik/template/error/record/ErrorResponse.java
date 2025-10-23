package com.benseddik.template.error.record;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Enveloppe standardisée pour toute réponse d'erreur.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String title,
        String detail,
        String path,
        Instant timestamp,
        String correlationId,
        List<FieldError> errors
) {
    public static ErrorResponse of(
            int status, String title, String detail, String path,
            String correlationId, List<FieldError> errors
    ) {
        return new ErrorResponse(status, title, detail, path, Instant.now(), correlationId, errors);
    }
}