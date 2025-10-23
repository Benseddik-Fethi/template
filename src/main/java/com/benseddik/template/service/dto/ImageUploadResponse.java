package com.benseddik.template.service.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Réponse après upload d'une image sur RustFS
 */
@Value
@Builder
@Schema(description = "Informations sur l'image uploadée")
public class ImageUploadResponse {

    @Schema(
            description = "URL publique complète de l'image",
            example = "https://rustfs.example.com/realms/users/123e4567-e89b-12d3-a456-426614174000.jpg"
    )
    String imageUrl;

    @Schema(
            description = "Nom du fichier original",
            example = "mon-chien.jpg"
    )
    String originalFilename;

    @Schema(
            description = "Nom du fichier généré (UUID)",
            example = "123e4567-e89b-12d3-a456-426614174000.jpg"
    )
    String generatedFilename;

    @Schema(
            description = "Dossier de destination",
            example = "dogs"
    )
    String folder;

    @Schema(
            description = "Taille du fichier en octets",
            example = "524288"
    )
    Long sizeBytes;

    @Schema(
            description = "Type MIME du fichier",
            example = "image/jpeg"
    )
    String mimeType;

    @Schema(
            description = "Timestamp de l'upload",
            example = "2025-01-15T10:30:00Z"
    )
    Instant uploadedAt;

    @Schema(
            description = "Nom d'utilisateur ayant uploadé",
            example = "john.doe"
    )
    String uploadedBy;
}