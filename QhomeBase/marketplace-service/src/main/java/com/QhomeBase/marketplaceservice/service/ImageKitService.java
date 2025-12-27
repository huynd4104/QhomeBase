package com.QhomeBase.marketplaceservice.service;

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
     * Upload a single image to ImageKit with retry logic
     * @param file The image file to upload
     * @param folder Optional folder path in ImageKit (e.g., "marketplace/posts", "marketplace/comments")
     * @return The URL of the uploaded image
     * @throws IOException if file cannot be read or upload fails after retries
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        int maxRetries = 3;
        long baseDelayMs = 1000; // Start with 1 second delay
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("📤 [ImageKit] Uploading image (attempt {}/{}): {} to folder: {}", 
                        attempt, maxRetries, file.getOriginalFilename(), folder);
                
                // Convert file to base64
                byte[] fileBytes = file.getBytes();
                String base64File = Base64.getEncoder().encodeToString(fileBytes);
                
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
                Result result = imageKit.upload(fileCreateRequest);
                
                if (result != null && result.getUrl() != null) {
                    log.info("✅ [ImageKit] Image uploaded successfully (attempt {}): {}", attempt, result.getUrl());
                    return result.getUrl();
                } else {
                    log.error("❌ [ImageKit] Upload failed (attempt {}): result is null or URL is null", attempt);
                    if (attempt == maxRetries) {
                        throw new IOException("Failed to upload image to ImageKit: no URL returned");
                    }
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                boolean isRetryableError = errorMessage != null && (
                        errorMessage.contains("FLOW_CONTROL_ERROR") ||
                        errorMessage.contains("stream was reset") ||
                        errorMessage.contains("Connection") ||
                        errorMessage.contains("timeout") ||
                        errorMessage.contains("network")
                );
                
                if (attempt < maxRetries && isRetryableError) {
                    long delayMs = baseDelayMs * (long) Math.pow(2, attempt - 1); // Exponential backoff
                    log.warn("⚠️ [ImageKit] Upload failed (attempt {}), retrying in {}ms: {}", 
                            attempt, delayMs, errorMessage);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Upload interrupted", ie);
                    }
                } else {
                    log.error("❌ [ImageKit] Error uploading image (attempt {}): {}", attempt, errorMessage, e);
                    throw new IOException("Error uploading image to ImageKit: " + errorMessage, e);
                }
            }
        }
        
        throw new IOException("Failed to upload image to ImageKit after " + maxRetries + " attempts");
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
     * Upload image from byte array to ImageKit with retry logic
     * @param imageBytes The image bytes to upload
     * @param fileName The filename (with extension) for the image
     * @param folder Optional folder path in ImageKit (e.g., "marketplace/posts", "marketplace/comments")
     * @return The URL of the uploaded image
     * @throws IOException if upload fails after retries
     */
    public String uploadImageFromBytes(byte[] imageBytes, String fileName, String folder) throws IOException {
        int maxRetries = 3;
        long baseDelayMs = 1000; // Start with 1 second delay
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("📤 [ImageKit] Uploading image from bytes (attempt {}/{}): {} to folder: {}", 
                        attempt, maxRetries, fileName, folder);
                
                // Convert bytes to base64
                String base64File = Base64.getEncoder().encodeToString(imageBytes);
                
                // Ensure filename has extension
                String fileExtension = fileName != null && fileName.contains(".")
                        ? fileName.substring(fileName.lastIndexOf("."))
                        : ".jpg";
                String finalFileName = fileName != null && fileName.contains(".")
                        ? fileName
                        : UUID.randomUUID().toString() + fileExtension;
                
                // Create upload request
                FileCreateRequest fileCreateRequest = new FileCreateRequest(base64File, finalFileName);
                fileCreateRequest.setUseUniqueFileName(true);
                
                // Set folder path if provided
                if (folder != null && !folder.isEmpty()) {
                    fileCreateRequest.setFolder(folder);
                }
                
                // Upload to ImageKit
                Result result = imageKit.upload(fileCreateRequest);
                
                if (result != null && result.getUrl() != null) {
                    log.info("✅ [ImageKit] Image uploaded successfully from bytes (attempt {}): {}", attempt, result.getUrl());
                    return result.getUrl();
                } else {
                    log.error("❌ [ImageKit] Upload failed (attempt {}): result is null or URL is null", attempt);
                    if (attempt == maxRetries) {
                        throw new IOException("Failed to upload image to ImageKit: no URL returned");
                    }
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                boolean isRetryableError = errorMessage != null && (
                        errorMessage.contains("FLOW_CONTROL_ERROR") ||
                        errorMessage.contains("stream was reset") ||
                        errorMessage.contains("Connection") ||
                        errorMessage.contains("timeout") ||
                        errorMessage.contains("network")
                );
                
                if (attempt < maxRetries && isRetryableError) {
                    long delayMs = baseDelayMs * (long) Math.pow(2, attempt - 1); // Exponential backoff
                    log.warn("⚠️ [ImageKit] Upload failed (attempt {}), retrying in {}ms: {}", 
                            attempt, delayMs, errorMessage);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Upload interrupted", ie);
                    }
                } else {
                    log.error("❌ [ImageKit] Error uploading image from bytes (attempt {}): {}", attempt, errorMessage, e);
                    throw new IOException("Error uploading image to ImageKit: " + errorMessage, e);
                }
            }
        }
        
        throw new IOException("Failed to upload image to ImageKit after " + maxRetries + " attempts");
    }

    /**
     * Get video streaming URL from ImageKit URL
     * ImageKit videos can be streamed directly, but we may need to add transformation parameters
     * or ensure the URL is in the correct format for ExoPlayer
     * 
     * For video streaming, ImageKit URLs work directly without transformation.
     * However, if hotlink protection is enabled, we may need to:
     * 1. Disable hotlink protection in ImageKit dashboard, OR
     * 2. Use signed URLs with expiration, OR
     * 3. Add transformation parameters to bypass protection
     * 
     * @param imageKitUrl The original ImageKit URL
     * @return The video URL optimized for streaming
     */
    public String getVideoStreamingUrl(String imageKitUrl) {
        if (imageKitUrl == null || imageKitUrl.isEmpty()) {
            return imageKitUrl;
        }
        
        // ImageKit URLs are already optimized for streaming
        // Return URL as-is - ImageKit handles video streaming natively
        // If 403 errors occur, check ImageKit dashboard for:
        // 1. Hotlink protection settings (should be disabled or allow your domain)
        // 2. Access control settings (should allow public access for videos)
        
        log.debug("📹 [ImageKit] Video streaming URL: {}", imageKitUrl);
        return imageKitUrl;
    }

    /**
     * Generate a signed URL for ImageKit resource to bypass hotlink protection
     * This uses ImageKit SDK to sign the URL with private key
     * 
     * @param imageKitUrl The original ImageKit URL
     * @param expireSeconds Optional expiration time in seconds (default: 3600 = 1 hour)
     * @return Signed URL that bypasses hotlink protection
     */
    public String getSignedUrl(String imageKitUrl, Long expireSeconds) {
        if (imageKitUrl == null || imageKitUrl.isEmpty()) {
            return imageKitUrl;
        }
        
        try {
            // Extract path from ImageKit URL
            // Example: https://ik.imagekit.io/fbxjffpvr/marketplace/posts/.../video.mp4
            // Path: marketplace/posts/.../video.mp4 (without leading slash)
            String urlEndpoint = imageKit.getConfig().getUrlEndpoint();
            String path = imageKitUrl;
            
            if (imageKitUrl.startsWith(urlEndpoint)) {
                path = imageKitUrl.substring(urlEndpoint.length());
                // Remove leading slash if present
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
            } else if (imageKitUrl.contains("ik.imagekit.io")) {
                // Extract path after domain
                // URL format: https://ik.imagekit.io/{imagekitId}/path/to/file
                int imagekitIdEnd = imageKitUrl.indexOf("/", imageKitUrl.indexOf("ik.imagekit.io") + "ik.imagekit.io".length());
                if (imagekitIdEnd > 0) {
                    int pathStart = imageKitUrl.indexOf("/", imagekitIdEnd + 1);
                    if (pathStart > 0) {
                        path = imageKitUrl.substring(pathStart + 1); // +1 to skip leading slash
                    }
                }
            }
            
            // Generate signed URL using ImageKit SDK
            java.util.Map<String, Object> options = new java.util.HashMap<>();
            options.put("path", path);
            options.put("signed", true);
            // ImageKit SDK expects Integer, not Long
            int expireSecondsInt = (expireSeconds != null && expireSeconds > 0) 
                    ? expireSeconds.intValue() 
                    : 3600; // Default: 1 hour
            options.put("expireSeconds", expireSecondsInt);
            
            String signedUrl = imageKit.getUrl(options);
            log.info("📹 [ImageKit] Generated signed URL (expires in {}s): {}", 
                    expireSeconds != null ? expireSeconds : 3600, signedUrl);
            return signedUrl;
            
        } catch (Exception e) {
            log.warn("⚠️ [ImageKit] Failed to generate signed URL, using original URL: {}", e.getMessage());
            log.debug("⚠️ [ImageKit] Exception details: ", e);
            return imageKitUrl; // Fallback to original URL
        }
    }

    /**
     * Check if URL is an ImageKit URL
     */
    public boolean isImageKitUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        // Check if URL contains ImageKit endpoint
        return url.contains("ik.imagekit.io") || url.contains("imagekit.io");
    }
}
