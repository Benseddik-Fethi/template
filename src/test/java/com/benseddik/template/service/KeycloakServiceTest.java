package com.benseddik.template.service;

import com.benseddik.template.domain.AppUser;
import com.benseddik.template.repository.AppUserRepository;
import com.benseddik.template.service.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KeycloakService - Unit Tests")
class KeycloakServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private Response response;

    @InjectMocks
    private KeycloakService keycloakService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakService, "realm", "test-realm");

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john.doe@example.com");
        registerRequest.setPassword("SecurePassword123!");

        // Setup mock chain
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
    }

    @Test
    @DisplayName("createUser - Should create user successfully")
    void createUser_Success() throws Exception {
        // Given
        when(usersResource.search(registerRequest.getEmail())).thenReturn(Collections.emptyList());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://keycloak/users/user-123"));
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(rolesResource.get("USER")).thenReturn(roleResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        keycloakService.createUser(registerRequest);

        // Then
        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usersResource).create(userCaptor.capture());

        UserRepresentation capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(capturedUser.getFirstName()).isEqualTo("John");
        assertThat(capturedUser.getLastName()).isEqualTo("Doe");
        assertThat(capturedUser.getUsername()).isEqualTo("john.doe");
        assertThat(capturedUser.isEnabled()).isTrue();
        assertThat(capturedUser.isEmailVerified()).isTrue();

        ArgumentCaptor<AppUser> appUserCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(appUserCaptor.capture());

        AppUser savedUser = appUserCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(savedUser.getDisplayName()).isEqualTo("John Doe");
        assertThat(savedUser.getExternalId()).isEqualTo("user-123");

        verify(response).close();
    }

    @Test
    @DisplayName("createUser - Should throw exception when email already exists")
    void createUser_EmailExists_ThrowsException() {
        // Given
        UserRepresentation existingUser = new UserRepresentation();
        existingUser.setEmail(registerRequest.getEmail());
        when(usersResource.search(registerRequest.getEmail())).thenReturn(List.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> keycloakService.createUser(registerRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Un compte existe déjà avec cet email")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usersResource, never()).create(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - Should throw exception on Keycloak creation failure")
    void createUser_KeycloakCreationFailure_ThrowsException() {
        // Given
        when(usersResource.search(registerRequest.getEmail())).thenReturn(Collections.emptyList());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(500);
        when(response.getStatusInfo()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR);

        // When & Then
        assertThatThrownBy(() -> keycloakService.createUser(registerRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Erreur lors de la création de l'utilisateur")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(userRepository, never()).save(any());
        verify(response).close();
    }

    @Test
    @DisplayName("deleteUser - Should delete user successfully")
    void deleteUser_Success() {
        // Given
        String externalId = "user-123";
        UserRepresentation userRep = new UserRepresentation();
        userRep.setEmail("test@example.com");

        when(usersResource.get(externalId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        when(usersResource.delete(externalId)).thenReturn(response);
        when(response.getStatus()).thenReturn(204);

        // When
        keycloakService.deleteUser(externalId);

        // Then
        verify(usersResource).delete(externalId);
        verify(response).close();
    }

    @Test
    @DisplayName("deleteUser - Should throw exception when externalId is null")
    void deleteUser_NullExternalId_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> keycloakService.deleteUser(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("L'identifiant externe de l'utilisateur est requis")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usersResource, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser - Should throw exception when externalId is blank")
    void deleteUser_BlankExternalId_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> keycloakService.deleteUser("   "))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("L'identifiant externe de l'utilisateur est requis")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(usersResource, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser - Should throw exception when user not found")
    void deleteUser_UserNotFound_ThrowsException() {
        // Given
        String externalId = "non-existent-user";
        when(usersResource.get(externalId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> keycloakService.deleteUser(externalId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Utilisateur non trouvé dans Keycloak")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(usersResource, never()).delete(any());
    }

    @Test
    @DisplayName("deleteUser - Should throw exception on deletion failure")
    void deleteUser_DeletionFailure_ThrowsException() {
        // Given
        String externalId = "user-123";
        UserRepresentation userRep = new UserRepresentation();
        userRep.setEmail("test@example.com");

        when(usersResource.get(externalId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        when(usersResource.delete(externalId)).thenReturn(response);
        when(response.getStatus()).thenReturn(500);

        // When & Then
        assertThatThrownBy(() -> keycloakService.deleteUser(externalId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Erreur lors de la suppression de l'utilisateur de Keycloak")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(response).close();
    }
}
