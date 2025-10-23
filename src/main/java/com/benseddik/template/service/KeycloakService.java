package com.benseddik.template.service;


import com.benseddik.template.domain.AppUser;
import com.benseddik.template.repository.AppUserRepository;
import com.benseddik.template.service.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakService {

    private final Keycloak keycloak;
    private final AppUserRepository userRepository;

    @Value("${keycloak.realm}")
    private String realm;

    public void createUser(RegisterRequest request) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            List<UserRepresentation> existingUsers = usersResource.search(request.getEmail());
            if (!existingUsers.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Un compte existe déjà avec cet email"
                );
            }

            UserRepresentation user = getUserRepresentation(request);
            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                log.info("✅ Utilisateur créé avec succès dans Keycloak: {} (ID: {})", request.getEmail(), userId);
                assignUserRole(realmResource, userId);
                AppUser appUser = new AppUser();
                appUser.setEmail(request.getEmail());
                appUser.setDisplayName(request.getFirstName() + " " + request.getLastName());
                appUser.setExternalId(userId);
                userRepository.save(appUser);
            } else {
                log.error("❌ Erreur lors de la création de l'utilisateur. Status: {}", response.getStatus());
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur lors de la création de l'utilisateur: " + response.getStatusInfo()
                );
            }
            response.close();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'utilisateur dans Keycloak", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la création de l'utilisateur: " + e.getMessage()
            );
        }
    }

    private static UserRepresentation getUserRepresentation(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getFirstName().toLowerCase() + "." + request.getLastName().toLowerCase());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));
        return user;
    }

    private void assignUserRole(RealmResource realmResource, String userId) {
        try {
            RoleRepresentation userRole = realmResource.roles().get("USER").toRepresentation();
            UserResource userResource = realmResource.users().get(userId);
            userResource.roles().realmLevel().add(Collections.singletonList(userRole));
            log.info("✅ Rôle 'user' assigné à l'utilisateur {}", userId);
        } catch (Exception e) {
            log.error("⚠️ Impossible d'assigner le rôle 'USER' à l'utilisateur {}: {}", userId, e.getMessage());
        }
    }
}