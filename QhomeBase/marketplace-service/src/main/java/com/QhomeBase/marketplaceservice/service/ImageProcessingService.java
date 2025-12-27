package com.QhomeBase.marketplaceservice.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for image processing (resize, thumbnail generation)
 */
@Service
@Slf4j
public class ImageProcessingService {

    private static final int THUMBNAIL_SIZE = 200;
    private static final int MEDIUM_SIZE = 400;
    private static final int LARGE_SIZE = 1200; // Increased for better quality
    private static final int MAX_ORIGINAL_SIZE = 4096; // Increased max size for original (only resize if larger)
    private static final double QUALITY = 1.0; // Maximum quality (1.0 = 100% quality, no compression)
    private static final double THUMBNAIL_QUALITY = 0.85; // Lower quality for thumbnails only

    /**
     * Process image and generate multiple sizes
     * Returns map with keys: original, thumbnail, medium, large
     */
    public Map<String, byte[]> processImage(MultipartFile file) throws IOException {
        log.debug("Processing image: {}", file.getOriginalFilename());
        
        Map<String, byte[]> processedImages = new HashMap<>();
        
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new IOException("Invalid image file");
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            // Original - only resize if too large, otherwise keep original
            if (originalWidth > MAX_ORIGINAL_SIZE || originalHeight > MAX_ORIGINAL_SIZE) {
                // Resize to max size while maintaining aspect ratio
                int newWidth = originalWidth;
                int newHeight = originalHeight;
                if (originalWidth > originalHeight) {
                    if (originalWidth > MAX_ORIGINAL_SIZE) {
                        newWidth = MAX_ORIGINAL_SIZE;
                        newHeight = (int) (originalHeight * ((double) MAX_ORIGINAL_SIZE / originalWidth));
                    }
                } else {
                    if (originalHeight > MAX_ORIGINAL_SIZE) {
                        newHeight = MAX_ORIGINAL_SIZE;
                        newWidth = (int) (originalWidth * ((double) MAX_ORIGINAL_SIZE / originalHeight));
                    }
                }
                processedImages.put("original", resizeImage(originalImage, newWidth, newHeight, QUALITY));
                log.info("Resized original image from {}x{} to {}x{}", originalWidth, originalHeight, newWidth, newHeight);
            } else {
                // Keep original quality
                processedImages.put("original", file.getBytes());
                log.info("Keeping original image size: {}x{}", originalWidth, originalHeight);
            }

            // Thumbnail (200x200) - lower quality for smaller file size
            processedImages.put("thumbnail", resizeImage(originalImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE, THUMBNAIL_QUALITY));

            // Medium (400x400) - for list view
            processedImages.put("medium", resizeImage(originalImage, MEDIUM_SIZE, MEDIUM_SIZE, QUALITY));

            // Large (1200x1200) - for detail view, high quality
            processedImages.put("large", resizeImage(originalImage, LARGE_SIZE, LARGE_SIZE, QUALITY));

            log.debug("Processed image into {} sizes", processedImages.size());
            return processedImages;
        }
    }

    /**
     * Resize image maintaining aspect ratio
     */
    private byte[] resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) throws IOException {
        return resizeImage(originalImage, maxWidth, maxHeight, QUALITY);
    }
    
    /**
     * Resize image maintaining aspect ratio with custom quality
     */
    private byte[] resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight, double quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        Thumbnails.of(originalImage)
                .size(maxWidth, maxHeight)
                .outputFormat("jpg")
                .outputQuality(quality)
                .toOutputStream(outputStream);
        
        return outputStream.toByteArray();
    }

    /**
     * Validate image file
     */
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Check file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Image size must be less than 5MB");
        }

        // Check allowed types
        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
        boolean isAllowed = false;
        for (String type : allowedTypes) {
            if (contentType.equals(type)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new IllegalArgumentException("Image type must be JPEG, PNG, or WebP");
        }
    }
}

