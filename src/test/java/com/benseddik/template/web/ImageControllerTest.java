package com.benseddik.template.web;

import com.benseddik.template.config.TestSecurityConfig;
import com.benseddik.template.service.RustFsService;
import com.benseddik.template.service.dto.ImageUploadResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ImageController - Integration Tests")
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RustFsService rustFsService;

    @Test
    @DisplayName("POST /images/users - Should upload image successfully")
    @WithMockUser(roles = "USER")
    void uploadUserImage_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        ImageUploadResponse mockResponse = ImageUploadResponse.builder()
                .imageUrl("https://s3.example.com/bucket/users/test-123.jpg")
                .originalFilename("test-image.jpg")
                .generatedFilename("test-123.jpg")
                .folder("users")
                .sizeBytes(18L)
                .mimeType("image/jpeg")
                .uploadedBy("testuser")
                .uploadedAt(Instant.now())
                .build();

        when(rustFsService.uploadFile(any(), eq("users"), any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .file(file)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://s3.example.com/bucket/users/test-123.jpg"))
                .andExpect(jsonPath("$.originalFilename").value("test-image.jpg"))
                .andExpect(jsonPath("$.folder").value("users"))
                .andExpect(jsonPath("$.sizeBytes").value(18))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"));

        verify(rustFsService).uploadFile(any(), eq("users"), any());
    }

    @Test
    @DisplayName("POST /images/users - Should return 401 without authentication")
    void uploadUserImage_Unauthorized() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/images/users").file(file))
                .andExpect(status().isUnauthorized());

        verify(rustFsService, never()).uploadFile(any(), any(), any());
    }

    @Test
    @DisplayName("POST /images/users - Should return 400 when file is missing")
    @WithMockUser(roles = "USER")
    void uploadUserImage_MissingFile_Returns400() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .with(jwt()))
                .andExpect(status().isBadRequest());

        verify(rustFsService, never()).uploadFile(any(), any(), any());
    }

    @Test
    @DisplayName("POST /images/users - Should return 400 when file is empty")
    @WithMockUser(roles = "USER")
    void uploadUserImage_EmptyFile_Returns400() throws Exception {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        when(rustFsService.uploadFile(any(), eq("users"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier est vide ou absent"));

        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .file(emptyFile)
                        .with(jwt()))
                .andExpect(status().isBadRequest());

        verify(rustFsService).uploadFile(any(), eq("users"), any());
    }

    @Test
    @DisplayName("POST /images/users - Should return 400 when file type is invalid")
    @WithMockUser(roles = "USER")
    void uploadUserImage_InvalidFileType_Returns400() throws Exception {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(rustFsService.uploadFile(any(), eq("users"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de fichier non autorisé"));

        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .file(invalidFile)
                        .with(jwt()))
                .andExpect(status().isBadRequest());

        verify(rustFsService).uploadFile(any(), eq("users"), any());
    }

    @Test
    @DisplayName("POST /images/users - Should return 400 when file is too large")
    @WithMockUser(roles = "USER")
    void uploadUserImage_FileTooLarge_Returns400() throws Exception {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-image.jpg",
                "image/jpeg",
                largeContent
        );

        when(rustFsService.uploadFile(any(), eq("users"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'image ne doit pas dépasser 10 MB"));

        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .file(largeFile)
                        .with(jwt()))
                .andExpect(status().isBadRequest());

        verify(rustFsService).uploadFile(any(), eq("users"), any());
    }

    @Test
    @DisplayName("DELETE /images/{folder}/{filename} - Should delete image successfully")
    @WithMockUser(roles = "USER")
    void deleteImage_Success() throws Exception {
        // Given
        doNothing().when(rustFsService).deleteFile("users", "test-123.jpg");

        // When & Then
        mockMvc.perform(delete("/images/users/test-123.jpg")
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(rustFsService).deleteFile("users", "test-123.jpg");
    }

    @Test
    @DisplayName("DELETE /images/{folder}/{filename} - Should return 401 without authentication")
    void deleteImage_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(delete("/images/users/test-123.jpg"))
                .andExpect(status().isUnauthorized());

        verify(rustFsService, never()).deleteFile(any(), any());
    }

    @Test
    @DisplayName("DELETE /images/{folder}/{filename} - Should return 404 when file not found")
    @WithMockUser(roles = "USER")
    void deleteImage_NotFound_Returns404() throws Exception {
        // Given
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier non trouvé"))
                .when(rustFsService).deleteFile("users", "non-existent.jpg");

        // When & Then
        mockMvc.perform(delete("/images/users/non-existent.jpg")
                        .with(jwt()))
                .andExpect(status().isNotFound());

        verify(rustFsService).deleteFile("users", "non-existent.jpg");
    }

    @Test
    @DisplayName("DELETE /images/{folder}/{filename} - Should handle special characters in filename")
    @WithMockUser(roles = "USER")
    void deleteImage_SpecialCharactersInFilename_Success() throws Exception {
        // Given
        String filename = "test-file%20with%20spaces.jpg";
        doNothing().when(rustFsService).deleteFile(eq("users"), anyString());

        // When & Then
        mockMvc.perform(delete("/images/users/" + filename)
                        .with(jwt()))
                .andExpect(status().isNoContent());

        verify(rustFsService).deleteFile(eq("users"), anyString());
    }

    @Test
    @DisplayName("POST /images/users - Should accept PNG format")
    @WithMockUser(roles = "USER")
    void uploadUserImage_PngFormat_Success() throws Exception {
        // Given
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                "png content".getBytes()
        );

        ImageUploadResponse mockResponse = ImageUploadResponse.builder()
                .imageUrl("https://s3.example.com/bucket/users/test-456.png")
                .originalFilename("test-image.png")
                .generatedFilename("test-456.png")
                .folder("users")
                .sizeBytes(11L)
                .mimeType("image/png")
                .uploadedBy("testuser")
                .uploadedAt(Instant.now())
                .build();

        when(rustFsService.uploadFile(any(), eq("users"), any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .file(pngFile)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("image/png"));

        verify(rustFsService).uploadFile(any(), eq("users"), any());
    }

    @Test
    @DisplayName("POST /images/users - Should accept WebP format")
    @WithMockUser(roles = "USER")
    void uploadUserImage_WebPFormat_Success() throws Exception {
        // Given
        MockMultipartFile webpFile = new MockMultipartFile(
                "file",
                "test-image.webp",
                "image/webp",
                "webp content".getBytes()
        );

        ImageUploadResponse mockResponse = ImageUploadResponse.builder()
                .imageUrl("https://s3.example.com/bucket/users/test-789.webp")
                .originalFilename("test-image.webp")
                .generatedFilename("test-789.webp")
                .folder("users")
                .sizeBytes(12L)
                .mimeType("image/webp")
                .uploadedBy("testuser")
                .uploadedAt(Instant.now())
                .build();

        when(rustFsService.uploadFile(any(), eq("users"), any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(multipart("/images/users")
                        .file(webpFile)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("image/webp"));

        verify(rustFsService).uploadFile(any(), eq("users"), any());
    }
}
