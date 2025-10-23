package com.benseddik.template.web;

import com.benseddik.template.config.TestSecurityConfig;
import com.benseddik.template.service.KeycloakService;
import com.benseddik.template.service.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController - Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KeycloakService keycloakService;

    @Test
    @DisplayName("POST /auth/register - Should register user successfully")
    void register_Success() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        doNothing().when(keycloakService).createUser(any(RegisterRequest.class));

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(keycloakService).createUser(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when email is missing")
    void register_MissingEmail_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when email format is invalid")
    void register_InvalidEmailFormat_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when password is missing")
    void register_MissingPassword_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when password is too short")
    void register_PasswordTooShort_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Short1!");  // Less than 8 characters
        request.setFirstName("John");
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when firstName is missing")
    void register_MissingFirstName_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when firstName is too short")
    void register_FirstNameTooShort_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("J");  // Less than 2 characters
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when lastName is missing")
    void register_MissingLastName_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when lastName is too short")
    void register_LastNameTooShort_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("D");  // Less than 2 characters

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when firstName is too long")
    void register_FirstNameTooLong_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("A".repeat(51));  // More than 50 characters
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when lastName is too long")
    void register_LastNameTooLong_Returns400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("D".repeat(51));  // More than 50 characters

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 415 when content type is missing")
    void register_MissingContentType_Returns415() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePassword123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType());

        verify(keycloakService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /auth/register - Should return 400 when JSON is malformed")
    void register_MalformedJson_Returns400() throws Exception {
        // Given
        String malformedJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(keycloakService, never()).createUser(any());
    }
}
