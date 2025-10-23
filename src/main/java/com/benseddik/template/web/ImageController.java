package com.benseddik.template.web;

import com.benseddik.template.service.RustFsService;
import com.benseddik.template.service.dto.ImageUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Images", description = "Upload et gestion des images via RustFS")
public class ImageController {

    private final RustFsService rustFsService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ImageUploadResponse> uploadUserImage(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        log.info("📸 Upload photo de profil - User: {}, Taille: {} bytes",
                auth.getName(), file.getSize());
        return ResponseEntity.ok(rustFsService.uploadFile(file, "users", auth));
    }

    @DeleteMapping("/{folder}/{filename}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Supprimer une image")
    public ResponseEntity<Void> deleteImage(
            @PathVariable String folder,
            @PathVariable String filename) {
        rustFsService.deleteFile(folder, filename);
        log.info("🗑️ Image supprimée : {}/{}", folder, filename);
        return ResponseEntity.noContent().build();
    }
}

