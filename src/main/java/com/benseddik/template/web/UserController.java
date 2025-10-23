package com.benseddik.template.web;

import com.benseddik.template.service.UserService;
import com.benseddik.template.service.dto.MeResponse;
import com.benseddik.template.service.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Gestion du profil utilisateur")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Récupérer le profil de l'utilisateur connecté",
            description = "Retourne les informations du JWT + données locales de l'utilisateur"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profil récupéré",
                    content = @Content(schema = @Schema(implementation = MeResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<MeResponse> getMe(Authentication auth) {
        log.debug("GET /users/me - User: {}", auth.getName());
        MeResponse response = userService.getCurrentUserProfile(auth);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Mettre à jour le profil",
            description = "Permet de modifier le nom d'affichage et la photo de profil"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profil mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    public ResponseEntity<Void> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication auth) {

        log.info("PUT /users/me - User: {}", auth.getName());
        userService.updateProfile(request, auth);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Supprimer son compte",
            description = "Supprime définitivement le compte utilisateur (soft delete)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Compte supprimé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "501", description = "Fonctionnalité non implémentée")
    })
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        log.warn("DELETE /users/me - User: {}", auth.getName());
        userService.deleteAccount(auth);
        return ResponseEntity.noContent().build();
    }
}