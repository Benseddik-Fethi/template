package com.benseddik.template.error.record;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Une erreur ciblée (champ ou règle d'objet).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FieldError(
        String entityName,
        String fieldName,
        String message,
        String code
) {}