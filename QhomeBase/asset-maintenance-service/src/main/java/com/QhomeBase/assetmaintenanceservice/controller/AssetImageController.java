package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.config.FileStorageProperties;
import com.QhomeBase.assetmaintenanceservice.dto.asset.AssetResponse;
import com.QhomeBase.assetmaintenanceservice.service.AssetImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/asset-maintenance/assets")
@RequiredArgsConstructor
public class AssetImageController {

    private final AssetImageService assetImageService;
    private final FileStorageProperties fileStorageProperties;

    @PostMapping("/{assetId}/upload-images")
    @PreAuthorize("@authz.canUpdateAsset()")
    public ResponseEntity<AssetResponse> uploadImages(
            @PathVariable UUID assetId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file is required");
        }

        AssetResponse response = assetImageService.uploadImages(assetId, files, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{assetId}/images")
    @PreAuthorize("@authz.canUpdateAsset()")
    public ResponseEntity<AssetResponse> deleteImage(
            @PathVariable UUID assetId,
            @RequestParam("imageUrl") String imageUrl,
            Authentication authentication) {
        
        AssetResponse response = assetImageService.deleteImage(assetId, imageUrl, authentication);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{assetId}/primary-image")
    @PreAuthorize("@authz.canUpdateAsset()")
    public ResponseEntity<AssetResponse> setPrimaryImage(
            @PathVariable UUID assetId,
            @RequestParam("imageUrl") String imageUrl,
            Authentication authentication) {
        
        AssetResponse response = assetImageService.setPrimaryImage(assetId, imageUrl, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/images/{imagePath:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String imagePath) {
        try {
            Path basePath = Paths.get(fileStorageProperties.getLocation()).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(imagePath).normalize();
            
            if (!filePath.startsWith(basePath)) {
                throw new RuntimeException("Invalid file path: " + imagePath);
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found: " + imagePath);
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: " + imagePath, e);
        }
    }
}

