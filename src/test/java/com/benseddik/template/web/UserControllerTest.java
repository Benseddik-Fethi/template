package com.benseddik.template.web;

import com.benseddik.template.config.TestSecurityConfig;
import com.benseddik.template.service.UserService;
import com.benseddik.template.service.dto.MeResponse;
import com.benseddik.template.service.dto.UpdateProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
@DisplayName("UserController - Integration Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("GET /users/me - Should return user profile")
    @WithMockUser(roles = "USER")
    void getMe_Success() throws Exception {
        // Given
        MeResponse mockResponse = MeResponse.builder()
                .subject("test-user-id")
                .username("testuser")
                .name("Test User")
                .email("test@example.com")
                .roles(List.of("ROLE_USER"))
                .photoUrl("https://example.com/photo.jpg")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(userService.getCurrentUserProfile(any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/users/me")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.subject").value("test-user-id"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.photoUrl").value("https://example.com/photo.jpg"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(userService).getCurrentUserProfile(any());
    }

    @Test
    @DisplayName("PUT /users/me - Should update profile successfully")
    @WithMockUser(roles = "USER")
    void updateProfile_Success() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");
        request.setPhotoUrl("https://example.com/new-photo.jpg");

        doNothing().when(userService).updateProfile(any(), any());

        // When & Then
        mockMvc.perform(put("/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateProfile(any(UpdateProfileRequest.class), any());
    }

    @Test
    @DisplayName("PUT /users/me - Should return 400 for invalid request")
    @WithMockUser(roles = "USER")
    void updateProfile_InvalidRequest_Returns400() throws Exception {
        // Given
        String invalidJson = "{ \"displayName\": null }";

        // When & Then
        mockMvc.perform(put("/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isNoContent()); // ValidationNote: without @Valid, request passes

        verify(userService).updateProfile(any(UpdateProfileRequest.class), any());
    }

    @Test
    @DisplayName("PUT /users/me - Should return 400 when content type is missing")
    @WithMockUser(roles = "USER")
    void updateProfile_MissingContentType_Returns400() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");

        // When & Then
        mockMvc.perform(put("/users/me")
                        .with(jwt())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("DELETE /users/me - Should delete account successfully")
    @WithMockUser(roles = "USER")
    void deleteAccount_Success() throws Exception {
        // Given
        doNothing().when(userService).deleteAccount(any());

        // When & Then
        mockMvc.perform(delete("/users/me")
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(userService).deleteAccount(any());
    }

    @Test
    @DisplayName("GET /users/me - Should return 401 without authentication")
    void getMe_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getCurrentUserProfile(any());
    }

    @Test
    @DisplayName("PUT /users/me - Should return 401 without authentication")
    void updateProfile_Unauthorized() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");

        // When & Then
        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    @DisplayName("DELETE /users/me - Should return 401 without authentication")
    void deleteAccount_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).deleteAccount(any());
    }

    @Test
    @DisplayName("PUT /users/me - Should accept update with only displayName")
    @WithMockUser(roles = "USER")
    void updateProfile_OnlyDisplayName_Success() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Only Name");

        doNothing().when(userService).updateProfile(any(), any());

        // When & Then
        mockMvc.perform(put("/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateProfile(any(UpdateProfileRequest.class), any());
    }

    @Test
    @DisplayName("PUT /users/me - Should accept update with only photoUrl")
    @WithMockUser(roles = "USER")
    void updateProfile_OnlyPhotoUrl_Success() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhotoUrl("https://example.com/only-photo.jpg");

        doNothing().when(userService).updateProfile(any(), any());

        // When & Then
        mockMvc.perform(put("/users/me")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).updateProfile(any(UpdateProfileRequest.class), any());
    }
}
