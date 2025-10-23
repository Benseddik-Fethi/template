package com.benseddik.template.service.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête de mise à jour du profil utilisateur")
public class UpdateProfileRequest {

    @Size(min = 2, max = 100)
    @Schema(description = "Nom d'affichage", example = "Jean Dupont")
    private String displayName;

    @Size(max = 500)
    @Schema(description = "URL de la photo de profil")
    private String photoUrl;
}
