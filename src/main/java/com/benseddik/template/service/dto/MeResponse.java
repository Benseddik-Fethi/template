package com.benseddik.template.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@Schema(description = "Identité JWT courante")
public class MeResponse {
    String subject;
    @Schema(description = "Nom d'utilisateur (preferred_username)", example = "alice")
    String username;
    @Schema(example = "alice alice")
    String name;
    @Schema(example = "alice@test.local", format = "email")
    String email;
    @Schema(description = "Rôles Spring Security", example = "[\"ROLE_USER\"]")
    List<String> roles;
    Instant issuedAt;
    Instant expiresAt;
    @Schema(description = "URL de la photo de profil")
    String photoUrl;
}
