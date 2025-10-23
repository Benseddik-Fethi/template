package com.benseddik.template.service;

import com.benseddik.template.config.RustFsProperties;
import com.benseddik.template.service.dto.ImageUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RustFsService - Unit Tests")
class RustFsServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private RustFsProperties rustFsProperties;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RustFsService rustFsService;

    @BeforeEach
    void setUp() {
        when(rustFsProperties.getBucketName()).thenReturn("test-bucket");
        when(rustFsProperties.getEndpoint()).thenReturn("https://s3.example.com");
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    @DisplayName("uploadFile - Should upload file successfully")
    void uploadFile_Success() {
        // Given
        byte[] fileContent = "test image content".getBytes();
        MultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                fileContent
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        // When
        ImageUploadResponse response = rustFsService.uploadFile(file, "users", authentication);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOriginalFilename()).isEqualTo("test-image.jpg");
        assertThat(response.getFolder()).isEqualTo("users");
        assertThat(response.getSizeBytes()).isEqualTo(fileContent.length);
        assertThat(response.getMimeType()).isEqualTo("image/jpeg");
        assertThat(response.getUploadedBy()).isEqualTo("testuser");
        assertThat(response.getImageUrl()).startsWith("https://s3.example.com/test-bucket/users/");
        assertThat(response.getImageUrl()).endsWith(".jpg");
        assertThat(response.getGeneratedFilename()).matches("[0-9a-f\\-]+\\.jpg");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).startsWith("users/");
        assertThat(capturedRequest.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when file is null")
    void uploadFile_NullFile_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(null, "users", authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Le fichier est vide ou absent")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when file is empty")
    void uploadFile_EmptyFile_ThrowsException() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(file, "users", authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Le fichier est vide ou absent")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when MIME type is invalid")
    void uploadFile_InvalidMimeType_ThrowsException() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(file, "users", authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Type de fichier non autorisé")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when file is too large")
    void uploadFile_FileTooLarge_ThrowsException() {
        // Given
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MultipartFile file = new MockMultipartFile(
                "file",
                "large-image.jpg",
                "image/jpeg",
                largeContent
        );

        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(file, "users", authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ne doit pas dépasser 10 MB")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when extension is invalid")
    void uploadFile_InvalidExtension_ThrowsException() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "image/jpeg",
                "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(file, "users", authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Extension de fichier non autorisée")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when folder is null")
    void uploadFile_NullFolder_ThrowsException() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(file, null, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Le dossier de destination est obligatoire")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadFile - Should throw exception when folder contains special characters")
    void uploadFile_InvalidFolderName_ThrowsException() {
        // Given
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> rustFsService.uploadFile(file, "users/../../etc", authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Le nom de dossier contient des caractères non autorisés")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("deleteFile - Should delete file successfully")
    void deleteFile_Success() {
        // Given
        String folder = "users";
        String filename = "test-123.jpg";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        // When
        rustFsService.deleteFile(folder, filename);

        // Then
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo("users/test-123.jpg");
    }

    @Test
    @DisplayName("deleteFile - Should throw exception when file not found")
    void deleteFile_FileNotFound_ThrowsException() {
        // Given
        String folder = "users";
        String filename = "non-existent.jpg";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        // When & Then
        assertThatThrownBy(() -> rustFsService.deleteFile(folder, filename))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Fichier non trouvé")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("deleteFile - Should throw exception when folder is null")
    void deleteFile_NullFolder_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> rustFsService.deleteFile(null, "test.jpg"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder et filename sont obligatoires")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("deleteFile - Should throw exception when filename is null")
    void deleteFile_NullFilename_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> rustFsService.deleteFile("users", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Folder et filename sont obligatoires")
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("fileExists - Should return true when file exists")
    void fileExists_FileExists_ReturnsTrue() {
        // Given
        String key = "users/test-123.jpg";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        // When
        boolean exists = rustFsService.fileExists(key);

        // Then
        assertThat(exists).isTrue();

        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(requestCaptor.capture());

        HeadObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo(key);
    }

    @Test
    @DisplayName("fileExists - Should return false when file not found")
    void fileExists_FileNotFound_ReturnsFalse() {
        // Given
        String key = "users/non-existent.jpg";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        // When
        boolean exists = rustFsService.fileExists(key);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("uploadFile - Should accept all valid image formats")
    void uploadFile_AllValidFormats_Success() {
        // Given & When & Then
        String[] validFormats = {"jpg", "jpeg", "png", "webp", "heic"};
        String[] validMimeTypes = {"image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic"};

        for (int i = 0; i < validFormats.length; i++) {
            MultipartFile file = new MockMultipartFile(
                    "file",
                    "test." + validFormats[i],
                    validMimeTypes[i],
                    "content".getBytes()
            );

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            assertThatCode(() -> rustFsService.uploadFile(file, "users", authentication))
                    .doesNotThrowAnyException();
        }

        verify(s3Client, times(validFormats.length))
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }
}
