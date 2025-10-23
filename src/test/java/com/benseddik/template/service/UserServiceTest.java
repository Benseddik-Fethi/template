package com.benseddik.template.service;

import com.benseddik.template.domain.AppUser;
import com.benseddik.template.repository.AppUserRepository;
import com.benseddik.template.security.CurrentUserService;
import com.benseddik.template.service.dto.MeResponse;
import com.benseddik.template.service.dto.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Unit Tests")
class UserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private AppUser testUser;
    private Jwt testJwt;

    @BeforeEach
    void setUp() {
        testUser = AppUser.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .displayName("Test User")
                .externalId("keycloak-123")
                .photoUrl("https://example.com/photo.jpg")
                .build();

        testJwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("keycloak-123")
                .claim("preferred_username", "testuser")
                .claim("name", "Test User")
                .claim("email", "test@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("getCurrentUserProfile - Should return user profile successfully")
    void getCurrentUserProfile_Success() {
        // Given
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(authentication.getPrincipal()).thenReturn(testJwt);
        when(authentication.getAuthorities()).thenReturn((List) authorities);
        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);

        // When
        MeResponse response = userService.getCurrentUserProfile(authentication);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSubject()).isEqualTo("keycloak-123");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getName()).isEqualTo("Test User");
        assertThat(response.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");

        verify(currentUserService).ensureCurrentUser(authentication);
    }

    @Test
    @DisplayName("getCurrentUserProfile - Should throw exception when principal is not JWT")
    void getCurrentUserProfile_InvalidPrincipal() {
        // Given
        when(authentication.getPrincipal()).thenReturn("not-a-jwt");

        // When & Then
        assertThatThrownBy(() -> userService.getCurrentUserProfile(authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid token")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(currentUserService, never()).ensureCurrentUser(any());
    }

    @Test
    @DisplayName("updateProfile - Should update display name successfully")
    void updateProfile_UpdateDisplayName_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Display Name");

        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        when(userRepository.save(any(AppUser.class))).thenReturn(testUser);

        // When
        userService.updateProfile(request, authentication);

        // Then
        assertThat(testUser.getDisplayName()).isEqualTo("New Display Name");
        verify(userRepository).save(testUser);
        verify(currentUserService).ensureCurrentUser(authentication);
    }

    @Test
    @DisplayName("updateProfile - Should update photo URL successfully")
    void updateProfile_UpdatePhotoUrl_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhotoUrl("https://example.com/new-photo.jpg");

        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        when(userRepository.save(any(AppUser.class))).thenReturn(testUser);

        // When
        userService.updateProfile(request, authentication);

        // Then
        assertThat(testUser.getPhotoUrl()).isEqualTo("https://example.com/new-photo.jpg");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateProfile - Should update both display name and photo URL")
    void updateProfile_UpdateBoth_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");
        request.setPhotoUrl("https://example.com/new-photo.jpg");

        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        when(userRepository.save(any(AppUser.class))).thenReturn(testUser);

        // When
        userService.updateProfile(request, authentication);

        // Then
        assertThat(testUser.getDisplayName()).isEqualTo("New Name");
        assertThat(testUser.getPhotoUrl()).isEqualTo("https://example.com/new-photo.jpg");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateProfile - Should not save when no changes detected")
    void updateProfile_NoChanges_NoSave() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Test User");  // Same as current
        request.setPhotoUrl("https://example.com/photo.jpg");  // Same as current

        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);

        // When
        userService.updateProfile(request, authentication);

        // Then
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateProfile - Should trim whitespace from display name")
    void updateProfile_TrimWhitespace_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("  New Name  ");

        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        when(userRepository.save(any(AppUser.class))).thenReturn(testUser);

        // When
        userService.updateProfile(request, authentication);

        // Then
        assertThat(testUser.getDisplayName()).isEqualTo("New Name");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateProfile - Should not update with blank display name")
    void updateProfile_BlankDisplayName_NoUpdate() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("   ");

        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);

        // When
        userService.updateProfile(request, authentication);

        // Then
        assertThat(testUser.getDisplayName()).isEqualTo("Test User");  // Unchanged
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAccount - Should delete user from Keycloak and database")
    void deleteAccount_Success() {
        // Given
        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        doNothing().when(keycloakService).deleteUser(testUser.getExternalId());
        doNothing().when(userRepository).delete(testUser);

        // When
        userService.deleteAccount(authentication);

        // Then
        verify(keycloakService).deleteUser("keycloak-123");
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteAccount - Should delete locally even if user not found in Keycloak")
    void deleteAccount_NotFoundInKeycloak_DeletesLocally() {
        // Given
        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(keycloakService).deleteUser(testUser.getExternalId());
        doNothing().when(userRepository).delete(testUser);

        // When
        userService.deleteAccount(authentication);

        // Then
        verify(keycloakService).deleteUser("keycloak-123");
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteAccount - Should skip Keycloak deletion if no external ID")
    void deleteAccount_NoExternalId_SkipsKeycloak() {
        // Given
        testUser.setExternalId(null);
        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        doNothing().when(userRepository).delete(testUser);

        // When
        userService.deleteAccount(authentication);

        // Then
        verify(keycloakService, never()).deleteUser(any());
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteAccount - Should skip Keycloak deletion if external ID is blank")
    void deleteAccount_BlankExternalId_SkipsKeycloak() {
        // Given
        testUser.setExternalId("   ");
        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        doNothing().when(userRepository).delete(testUser);

        // When
        userService.deleteAccount(authentication);

        // Then
        verify(keycloakService, never()).deleteUser(any());
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("deleteAccount - Should throw exception on Keycloak error (not 404)")
    void deleteAccount_KeycloakError_ThrowsException() {
        // Given
        when(currentUserService.ensureCurrentUser(authentication)).thenReturn(testUser);
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Keycloak error"))
                .when(keycloakService).deleteUser(testUser.getExternalId());

        // When & Then
        assertThatThrownBy(() -> userService.deleteAccount(authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Keycloak error")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(keycloakService).deleteUser("keycloak-123");
        verify(userRepository, never()).delete(any());
    }
}
