package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.client.VideoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * File storage service
 * Currently uses local storage, can be extended to use S3/Firebase Storage
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${marketplace.upload.directory:uploads/marketplace}")
    private String uploadDirectory;

    @Value("${marketplace.cdn.base-url:}")
    private String cdnBaseUrl;

    private final ImageKitService imageKitService;
    private final VideoClient videoClient;

    public FileStorageService(ImageKitService imageKitService, VideoClient videoClient) {
        this.imageKitService = imageKitService;
        this.videoClient = videoClient;
    }

    /**
     * Upload image to ImageKit and return URL
     * Returns map with key: original (ImageKit URL)
     */
    public Map<String, String> uploadImage(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading image to ImageKit for post: {}", postId);
        
        // Upload to ImageKit with folder "marketplace/posts/{postId}"
        String imageUrl = imageKitService.uploadImage(file, "marketplace/posts/" + postId);

        Map<String, String> imageUrls = new HashMap<>();
        imageUrls.put("original", imageUrl);

        log.info("✅ [FileStorageService] Uploaded image to ImageKit: {}", imageUrl);
        return imageUrls;
    }

    /**
     * Upload video to VideoStorageService (data-docs-service)
     * Returns map with key: original (backend streaming URL)
     * NO LONGER USES IMAGEKIT - videos now stored in backend
     */
    public Map<String, String> uploadVideo(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading video to VideoStorageService for post: {}", postId);
        
        try {
            // Upload video to data-docs-service VideoStorageService
            // Assume uploadedBy is extracted from security context (for now use dummy UUID)
            UUID uploadedBy = UUID.randomUUID(); // TODO: Get from SecurityContext
            
            Map<String, Object> uploadResponse = videoClient.uploadVideo(
                    file,
                    "marketplace_post",  // category
                    UUID.fromString(postId),  // ownerId (post ID)
                    uploadedBy
            );
            
            // Extract videoId from response
            String videoId = uploadResponse.get("videoId").toString();
            
            // Get streaming URL from data-docs-service
            String streamingUrl = videoClient.getVideoStreamingUrl(UUID.fromString(videoId));

            Map<String, String> videoUrls = new HashMap<>();
            videoUrls.put("original", streamingUrl);
            videoUrls.put("videoId", videoId);  // Also return videoId for reference

            log.info("✅ [FileStorageService] Uploaded video to VideoStorageService: videoId={}, streamingUrl={}", 
                    videoId, streamingUrl);
            return videoUrls;
        } catch (Exception e) {
            log.error("❌ [FileStorageService] Failed to upload video to VideoStorageService", e);
            throw new IOException("Failed to upload video: " + e.getMessage(), e);
        }
    }

    /**
     * Upload processed images (thumbnail, medium, large) to ImageKit
     * Uploads both original and thumbnail if available
     */
    public Map<String, String> uploadProcessedImages(Map<String, byte[]> processedImages, String postId, String baseFileName) throws IOException {
        log.info("📤 [FileStorageService] Uploading processed images to ImageKit for post: {}", postId);
        
        Map<String, String> imageUrls = new HashMap<>();
        String folder = "marketplace/posts/" + postId;
        
        // Determine file extension from baseFileName
        String fileExtension = baseFileName != null && baseFileName.contains(".")
                ? baseFileName.substring(baseFileName.lastIndexOf("."))
                : ".jpg";
        
        // Upload original image if available
        if (processedImages.containsKey("original")) {
            byte[] originalBytes = processedImages.get("original");
            String originalFileName = baseFileName != null ? baseFileName : UUID.randomUUID().toString() + fileExtension;
            String originalUrl = imageKitService.uploadImageFromBytes(originalBytes, originalFileName, folder);
            imageUrls.put("original", originalUrl);
            log.info("✅ [FileStorageService] Uploaded original image: {}", originalUrl);
        }
        
        // Upload thumbnail if available
        if (processedImages.containsKey("thumbnail")) {
            byte[] thumbnailBytes = processedImages.get("thumbnail");
            String thumbnailFileName = baseFileName != null 
                    ? baseFileName.replace(fileExtension, "_thumb" + fileExtension)
                    : UUID.randomUUID().toString() + "_thumb" + fileExtension;
            String thumbnailUrl = imageKitService.uploadImageFromBytes(thumbnailBytes, thumbnailFileName, folder);
            imageUrls.put("thumbnail", thumbnailUrl);
            log.info("✅ [FileStorageService] Uploaded thumbnail image: {}", thumbnailUrl);
        }
        
        // Ensure at least original URL is present
        if (imageUrls.isEmpty()) {
            throw new IOException("No processed images to upload. Processed images map is empty or doesn't contain 'original' key.");
        }
        
        log.info("✅ [FileStorageService] Successfully uploaded {} processed image(s) to ImageKit", imageUrls.size());
        return imageUrls;
    }

    /**
     * Delete image files
     */
    public void deleteImage(String postId, String fileName) {
        try {
            Path filePath = Paths.get(uploadDirectory, postId, fileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted image: {}", filePath);
        } catch (IOException e) {
            log.error("Error deleting image: {}", fileName, e);
        }
    }

    /**
     * Delete all images for a post
     */
    public void deletePostImages(String postId) {
        try {
            Path postPath = Paths.get(uploadDirectory, postId);
            if (Files.exists(postPath)) {
                Files.walk(postPath)
                        .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Error deleting file: {}", path, e);
                            }
                        });
                log.info("Deleted all images for post: {}", postId);
            }
        } catch (IOException e) {
            log.error("Error deleting post images: {}", postId, e);
        }
    }

    /**
     * Get image URL (with CDN if configured)
     */
    private String getImageUrl(String postId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            // Use CDN URL
            return String.format("%s/marketplace/%s/%s", cdnBaseUrl, postId, fileName);
        } else {
            // Use local URL (for development)
            return String.format("/api/marketplace/uploads/%s/%s", postId, fileName);
        }
    }

    /**
     * Upload comment image to ImageKit
     * Returns the image URL
     */
    public String uploadCommentImage(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading comment image to ImageKit for post: {}", postId);
        
        // Upload to ImageKit with folder "marketplace/comments/{postId}"
        String imageUrl = imageKitService.uploadImage(file, "marketplace/comments/" + postId);
        
        log.info("✅ [FileStorageService] Uploaded comment image to ImageKit: {}", imageUrl);
        return imageUrl;
    }

    /**
     * Upload comment video to ImageKit
     * Returns the video URL optimized for streaming
     */
    public String uploadCommentVideo(MultipartFile file, String postId) throws IOException {
        log.info("📤 [FileStorageService] Uploading comment video to VideoStorageService for post: {}", postId);

        try {
            // Upload video to data-docs-service VideoStorageService
            UUID uploadedBy = UUID.randomUUID(); // TODO: Get from SecurityContext
            
            Map<String, Object> uploadResponse = videoClient.uploadVideo(
                    file,
                    "marketplace_comment",  // category
                    UUID.fromString(postId),  // ownerId (post ID)
                    uploadedBy
            );
            
            // Extract videoId from response
            String videoId = uploadResponse.get("videoId").toString();
            
            // Get streaming URL
            String streamingUrl = videoClient.getVideoStreamingUrl(UUID.fromString(videoId));

            log.info("✅ [FileStorageService] Uploaded comment video to VideoStorageService: videoId={}, streamingUrl={}", 
                    videoId, streamingUrl);
            return streamingUrl;
        } catch (Exception e) {
            log.error("❌ [FileStorageService] Failed to upload comment video to VideoStorageService", e);
            throw new IOException("Failed to upload comment video: " + e.getMessage(), e);
        }
    }

    /**
     * Get comment image URL
     */
    private String getCommentImageUrl(String postId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return String.format("%s/marketplace/%s/comments/%s", cdnBaseUrl, postId, fileName);
        } else {
            return String.format("/api/marketplace/uploads/%s/comments/%s", postId, fileName);
        }
    }

    /**
     * Get comment video URL
     */
    private String getCommentVideoUrl(String postId, String fileName) {
        if (cdnBaseUrl != null && !cdnBaseUrl.isEmpty()) {
            return String.format("%s/marketplace/%s/comments/%s", cdnBaseUrl, postId, fileName);
        } else {
            return String.format("/api/marketplace/uploads/%s/comments/%s", postId, fileName);
        }
    }

    /**
     * Get image file path
     */
    public Path getImagePath(String postId, String fileName) {
        Path path = Paths.get(uploadDirectory, postId, fileName);
        log.debug("📁 [FileStorageService] Getting image path: postId={}, fileName={}, fullPath={}, exists={}", 
                postId, fileName, path, java.nio.file.Files.exists(path));
        return path;
    }

    /**
     * Get comment image/video file path
     */
    public Path getCommentFilePath(String postId, String fileName) {
        Path path = Paths.get(uploadDirectory, postId, "comments", fileName);
        log.debug("📁 [FileStorageService] Getting comment file path: postId={}, fileName={}, fullPath={}, exists={}", 
                postId, fileName, path, java.nio.file.Files.exists(path));
        return path;
    }
}

