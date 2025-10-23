package com.benseddik.template.service;

import com.benseddik.template.config.RustFsProperties;
import com.benseddik.template.service.dto.ImageUploadResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service pour gérer les uploads/suppressions vers RustFS (S3-compatible)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RustFsService {

    private final S3Client s3Client;
    private final RustFsProperties rustFsProperties;

    // Configuration
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "heic");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic"
    );

    /**
     * Vérifier la disponibilité du bucket au démarrage
     */
    @PostConstruct
    public void init() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .build();

            s3Client.headBucket(headBucketRequest);
            log.info("RustFS connecté - Bucket '{}' disponible sur {}",
                    rustFsProperties.getBucketName(),
                    rustFsProperties.getEndpoint());

        } catch (NoSuchBucketException e) {
            log.error("Bucket '{}' introuvable. Créez-le dans l'interface RustFS.",
                    rustFsProperties.getBucketName());
            throw new IllegalStateException("Bucket RustFS non disponible: " + rustFsProperties.getBucketName(), e);
        } catch (Exception e) {
            log.warn("Impossible de vérifier le bucket RustFS: {}", e.getMessage());
        }
    }

    /**
     * Upload un fichier vers RustFS
     *
     * @param file   Fichier à uploader
     * @param folder Dossier de destination (dogs, walks, users, etc.)
     * @param auth   Authentification de l'utilisateur
     * @return Informations sur l'upload
     */
    public ImageUploadResponse uploadFile(MultipartFile file, String folder, Authentication auth) {
        // Validation
        validateFile(file);
        validateFolder(folder);

        // Génération du nom de fichier unique
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String generatedFilename = UUID.randomUUID() + "." + fileExtension;
        String key = folder + "/" + generatedFilename;

        try {
            // Upload vers RustFS
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // Construire l'URL publique
            String fileUrl = buildPublicUrl(key);

            log.info("Fichier uploadé sur RustFS - User: {}, Key: {}, Size: {} bytes",
                    auth != null ? auth.getName() : "system",
                    key,
                    file.getSize());

            return ImageUploadResponse.builder()
                    .imageUrl(fileUrl)
                    .originalFilename(originalFilename)
                    .generatedFilename(generatedFilename)
                    .folder(folder)
                    .sizeBytes(file.getSize())
                    .mimeType(file.getContentType())
                    .uploadedAt(Instant.now())
                    .uploadedBy(auth != null ? auth.getName() : "system")
                    .build();

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier vers RustFS", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'upload du fichier: " + e.getMessage()
            );
        } catch (S3Exception e) {
            log.error("Erreur S3 lors de l'upload: {}", e.awsErrorDetails().errorMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur RustFS: " + e.awsErrorDetails().errorMessage()
            );
        }
    }

    /**
     * Supprimer un fichier de RustFS
     *
     * @param folder   Dossier (dogs, walks, users)
     * @param filename Nom du fichier
     */
    public void deleteFile(String folder, String filename) {
        if (folder == null || folder.isBlank() || filename == null || filename.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Folder et filename sont obligatoires"
            );
        }

        String key = folder + "/" + filename;

        try {
            // Vérifier que le fichier existe avant de supprimer
            if (!fileExists(key)) {
                log.warn("Tentative de suppression d'un fichier inexistant: {}", key);
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Fichier non trouvé: " + key
                );
            }

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Fichier supprimé de RustFS: {}", key);

        } catch (ResponseStatusException e) {
            throw e; // Repropager les erreurs 404/400
        } catch (S3Exception e) {
            log.error("Erreur S3 lors de la suppression de {}: {}",
                    key, e.awsErrorDetails().errorMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la suppression du fichier: " + e.awsErrorDetails().errorMessage()
            );
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la suppression de {}", key, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur inattendue lors de la suppression du fichier"
            );
        }
    }

    /**
     * Vérifier si un fichier existe sur RustFS
     *
     * @param key Clé du fichier (folder/filename)
     * @return true si le fichier existe
     */
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification d'existence de {}: {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Construire l'URL publique d'un fichier
     */
    private String buildPublicUrl(String key) {
        return String.format("%s/%s/%s",
                rustFsProperties.getEndpoint(),
                rustFsProperties.getBucketName(),
                key
        );
    }

    /**
     * Valider le fichier uploadé
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le fichier est vide ou absent"
            );
        }

        // Vérifier le type MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Type de fichier non autorisé. Types acceptés: %s",
                            String.join(", ", ALLOWED_MIME_TYPES))
            );
        }

        // Vérifier la taille
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("L'image ne doit pas dépasser %d MB (taille actuelle: %.2f MB)",
                            MAX_FILE_SIZE / 1024 / 1024,
                            file.getSize() / 1024.0 / 1024.0)
            );
        }

        // Vérifier l'extension
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(filename);

        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Extension de fichier non autorisée. Extensions acceptées: %s",
                            String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
    }

    /**
     * Valider le nom de dossier
     */
    private void validateFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le dossier de destination est obligatoire"
            );
        }

        // Nettoyer le nom de dossier (pas de caractères spéciaux)
        if (!folder.matches("^[a-zA-Z0-9_-]+$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom de dossier contient des caractères non autorisés. Utilisez uniquement: a-z, A-Z, 0-9, _, -"
            );
        }
    }

    /**
     * Extraire l'extension d'un nom de fichier
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }
}