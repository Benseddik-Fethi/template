package com.benseddik.template.web;


import com.benseddik.template.service.KeycloakService;
import com.benseddik.template.service.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final KeycloakService keycloakService;

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur dans Keycloak")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        keycloakService.createUser(request);
        return ResponseEntity.ok().build();
    }
}