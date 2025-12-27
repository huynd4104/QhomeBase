package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;
    private Path fileStorageLocation;

    public void init() {
        try {
            this.fileStorageLocation = Paths.get(fileStorageProperties.getLocation())
                    .toAbsolutePath()
                    .normalize();
            Files.createDirectories(this.fileStorageLocation.resolve("asset-maintenance").resolve("assets"));
            log.info("File storage location initialized at: {} (asset-maintenance service)", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not initialize file storage location", ex);
        }
    }

    public String uploadAssetImage(MultipartFile file, UUID assetId) {
        validateImageFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Invalid file path: " + fileName);
            }

            Path targetLocation = this.fileStorageLocation
                    .resolve("asset-maintenance")
                    .resolve("assets")
                    .resolve(assetId.toString())
                    .resolve(fileName);

            Files.createDirectories(targetLocation.getParent());

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "asset-maintenance/assets/" + assetId + "/" + fileName;
            log.info("File uploaded successfully: {}", relativePath);
            return relativePath;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file: " + fileName, ex);
        }
    }

    public List<String> uploadMultipleAssetImages(List<MultipartFile> files, UUID assetId) {
        List<String> uploadedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            String path = uploadAssetImage(file, assetId);
            uploadedPaths.add(path);
        }
        return uploadedPaths;
    }

    public void deleteAssetImage(String imagePath, UUID assetId) {
        try {
            Path filePath = this.fileStorageLocation.resolve(imagePath).normalize();
            
            if (!filePath.startsWith(this.fileStorageLocation.normalize())) {
                throw new RuntimeException("Invalid file path: " + imagePath);
            }
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", imagePath);
            }
        } catch (IOException ex) {
            log.error("Could not delete file: {}", imagePath, ex);
            throw new RuntimeException("Could not delete file: " + imagePath, ex);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isImageContentType(contentType)) {
            throw new RuntimeException("File must be an image (JPEG, PNG, GIF, WEBP)");
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size exceeds maximum limit of 10MB");
        }
    }

    private boolean isImageContentType(String contentType) {
        return contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }
}

