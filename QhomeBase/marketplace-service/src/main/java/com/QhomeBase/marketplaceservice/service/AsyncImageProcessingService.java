package com.QhomeBase.marketplaceservice.service;

import com.QhomeBase.marketplaceservice.model.MarketplacePost;
import com.QhomeBase.marketplaceservice.model.MarketplacePostImage;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostImageRepository;
import com.QhomeBase.marketplaceservice.repository.MarketplacePostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async service for image processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncImageProcessingService {

    private final ImageProcessingService imageProcessingService;
    private final FileStorageService fileStorageService;
    private final MarketplacePostImageRepository imageRepository;
    private final MarketplacePostRepository postRepository;

    /**
     * Process image asynchronously and save to database
     * Note: MultipartFile bytes are copied before async processing to avoid file deletion issues
     */
    @Async("imageProcessingExecutor")
    @Transactional
    public CompletableFuture<MarketplacePostImage> processImageAsync(
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            UUID postId, 
            String baseFileName,
            int sortOrder) {
        try {
            log.info("Starting async image processing for post: {}, sortOrder: {}", postId, sortOrder);
            
            // Get post
            MarketplacePost post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
            
            // Create a temporary MultipartFile-like wrapper from bytes
            MultipartFileWrapper fileWrapper = new MultipartFileWrapper(
                    fileBytes, originalFilename, contentType);
            
            // Validate image
            imageProcessingService.validateImage(fileWrapper);
            
            // Process image (resize, thumbnail)
            Map<String, byte[]> processedImages = imageProcessingService.processImage(fileWrapper);
            
            // Upload processed images
            Map<String, String> imageUrls = fileStorageService.uploadProcessedImages(
                    processedImages, postId.toString(), baseFileName);
            
            // Get URLs
            String imageUrl = imageUrls.get("original");
            String thumbnailUrl = imageUrls.get("thumbnail");
            
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new RuntimeException("Failed to upload image: no URL returned");
            }
            
            // Create and save MarketplacePostImage entity
            MarketplacePostImage postImage = MarketplacePostImage.builder()
                    .post(post)
                    .imageUrl(imageUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .sortOrder(sortOrder)
                    .build();
            
            MarketplacePostImage saved = imageRepository.save(postImage);
            
            log.info("Completed async image processing and saved to database for post: {}, imageId: {}, imageUrl: {}", 
                    postId, saved.getId(), imageUrl);
            
            return CompletableFuture.completedFuture(saved);
            
        } catch (Exception e) {
            log.error("Error processing image asynchronously for post: {}", postId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process thumbnail asynchronously and update existing image
     * This is used when image is already uploaded and saved to database
     */
    @Async("imageProcessingExecutor")
    @Transactional
    public CompletableFuture<Void> processThumbnailAsync(
            byte[] fileBytes,
            String originalFilename,
            String contentType,
            UUID postId,
            String baseFileName,
            UUID existingImageId) {
        try {
            log.info("Starting async thumbnail processing for image: {}, post: {}", existingImageId, postId);
            
            // Get existing image
            MarketplacePostImage existingImage = imageRepository.findById(existingImageId)
                    .orElseThrow(() -> new RuntimeException("Image not found: " + existingImageId));
            
            // Create a temporary MultipartFile-like wrapper from bytes
            MultipartFileWrapper fileWrapper = new MultipartFileWrapper(
                    fileBytes, originalFilename, contentType);
            
            // Validate image
            imageProcessingService.validateImage(fileWrapper);
            
            // Process image (resize, thumbnail)
            Map<String, byte[]> processedImages = imageProcessingService.processImage(fileWrapper);
            
            // Upload processed images (thumbnail)
            Map<String, String> imageUrls = fileStorageService.uploadProcessedImages(
                    processedImages, postId.toString(), baseFileName);
            
            // Get thumbnail URL
            String thumbnailUrl = imageUrls.get("thumbnail");
            
            // Update existing image with thumbnail URL
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                existingImage.setThumbnailUrl(thumbnailUrl);
                imageRepository.save(existingImage);
                log.info("✅ [AsyncImageProcessingService] Updated thumbnail for image: {}, thumbnailUrl: {}", 
                        existingImageId, thumbnailUrl);
            } else {
                log.warn("⚠️ [AsyncImageProcessingService] No thumbnail URL returned for image: {}", existingImageId);
            }
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Error processing thumbnail asynchronously for image: {}", existingImageId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Simple wrapper to convert bytes to MultipartFile-like interface
     */
    private static class MultipartFileWrapper implements MultipartFile {
        private final byte[] bytes;
        private final String name;
        private final String contentType;

        public MultipartFileWrapper(byte[] bytes, String name, String contentType) {
            this.bytes = bytes;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes == null || bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes != null ? bytes.length : 0;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return bytes;
        }

        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}

