package com.QhomeBase.datadocsservice.service;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.models.FileCreateRequest;
import io.imagekit.sdk.models.results.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageKitService {

    private final ImageKit imageKit;

    /**
     * Upload a single image to ImageKit
     * @param file The image file to upload
     * @param folder Optional folder path in ImageKit (e.g., "household", "vehicle", "chat")
     * @return The URL of the uploaded image
     * @throws IOException if file cannot be read
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        try {
            log.info("📤 [ImageKit] Uploading image: {} to folder: {}", file.getOriginalFilename(), folder);
            
            // Validate file
            if (file == null || file.isEmpty()) {
                log.error("❌ [ImageKit] File is null or empty");
                throw new IllegalArgumentException("File cannot be null or empty");
            }
            
            // Validate file size (max 50MB before base64 encoding)
            long fileSize = file.getSize();
            if (fileSize > 50 * 1024 * 1024) {
                log.error("❌ [ImageKit] File too large: {} bytes (max 50MB)", fileSize);
                throw new IllegalArgumentException("File size exceeds maximum limit of 50MB");
            }
            
            // Convert file to base64
            log.debug("📤 [ImageKit] Reading file bytes (size: {} bytes)...", fileSize);
            byte[] fileBytes;
            try {
                fileBytes = file.getBytes();
            } catch (IOException e) {
                log.error("❌ [ImageKit] Failed to read file bytes: {}", e.getMessage(), e);
                throw new IOException("Failed to read file: " + e.getMessage(), e);
            }
            
            if (fileBytes == null || fileBytes.length == 0) {
                log.error("❌ [ImageKit] File bytes are null or empty");
                throw new IllegalArgumentException("File bytes cannot be null or empty");
            }
            
            log.debug("📤 [ImageKit] Encoding {} bytes to base64...", fileBytes.length);
            String base64File;
            try {
                base64File = Base64.getEncoder().encodeToString(fileBytes);
                log.debug("📤 [ImageKit] Base64 encoding completed (length: {} chars)", base64File.length());
            } catch (Exception e) {
                log.error("❌ [ImageKit] Failed to encode file to base64: {}", e.getMessage(), e);
                throw new IOException("Failed to encode file to base64: " + e.getMessage(), e);
            }
            
            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // Create upload request
            FileCreateRequest fileCreateRequest = new FileCreateRequest(base64File, fileName);
            fileCreateRequest.setUseUniqueFileName(true);
            
            // Set folder path if provided
            if (folder != null && !folder.isEmpty()) {
                fileCreateRequest.setFolder(folder);
            }
            
            // Upload to ImageKit
            log.debug("📤 [ImageKit] Calling imageKit.upload() with fileName: {}, folder: {}", fileName, folder);
            Result result;
            try {
                result = imageKit.upload(fileCreateRequest);
                log.debug("📤 [ImageKit] Upload call completed, result: {}", result != null ? "not null" : "null");
            } catch (RuntimeException e) {
                log.error("❌ [ImageKit] RuntimeException from ImageKit SDK: {}", e.getMessage(), e);
                log.error("❌ [ImageKit] Exception class: {}", e.getClass().getName());
                if (e.getCause() != null) {
                    log.error("❌ [ImageKit] Caused by: {}", e.getCause().getMessage(), e.getCause());
                }
                throw new IOException("ImageKit upload failed: " + e.getMessage(), e);
            }
            
            if (result == null) {
                String errorMessage = "Upload failed: ImageKit returned null result";
                log.error("❌ [ImageKit] {}", errorMessage);
                throw new IOException("Failed to upload image to ImageKit: " + errorMessage);
            }
            
            if (result.getUrl() == null || result.getUrl().isEmpty()) {
                // Log additional info from result if available
                log.error("❌ [ImageKit] Upload failed: URL is null or empty");
                log.error("❌ [ImageKit] Result toString: {}", result.toString());
                String errorMessage = "Upload failed: ImageKit returned result without URL";
                throw new IOException("Failed to upload image to ImageKit: " + errorMessage);
            }
            
                log.info("✅ [ImageKit] Image uploaded successfully: {}", result.getUrl());
                return result.getUrl();
        } catch (IllegalArgumentException e) {
            log.error("❌ [ImageKit] Invalid argument: {}", e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            log.error("❌ [ImageKit] IO error uploading image: {}", e.getMessage(), e);
            throw e;
        } catch (RuntimeException e) {
            log.error("❌ [ImageKit] RuntimeException uploading image: {}", e.getMessage(), e);
            log.error("❌ [ImageKit] Exception class: {}", e.getClass().getName());
            throw new IOException("Error uploading image to ImageKit: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ [ImageKit] Unexpected error uploading image: {}", e.getMessage(), e);
            log.error("❌ [ImageKit] Exception class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("❌ [ImageKit] Caused by: {}", e.getCause().getMessage(), e.getCause());
            }
            throw new IOException("Error uploading image to ImageKit: " + e.getMessage(), e);
        }
    }

    /**
     * Upload multiple images to ImageKit
     * @param files List of image files to upload
     * @param folder Optional folder path in ImageKit
     * @return List of URLs of uploaded images
     * @throws IOException if any file cannot be read or uploaded
     */
    public List<String> uploadImages(List<MultipartFile> files, String folder) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = uploadImage(file, folder);
            imageUrls.add(url);
        }
        log.info("✅ [ImageKit] Uploaded {} images successfully", imageUrls.size());
        return imageUrls;
    }

    /**
     * Upload a file to ImageKit
     * NOTE: This method is for IMAGES ONLY. Video uploads should use VideoStorageService.
     * @param file The image file to upload (NOT video)
     * @param folder Optional folder path in ImageKit
     * @return The URL of the uploaded image
     * @throws IOException if file cannot be read
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        return uploadImage(file, folder); // Images only - videos should NOT use ImageKit
    }
}
