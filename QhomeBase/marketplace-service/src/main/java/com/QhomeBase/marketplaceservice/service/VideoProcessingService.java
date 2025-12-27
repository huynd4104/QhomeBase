package com.QhomeBase.marketplaceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for video validation
 */
@Service
@Slf4j
public class VideoProcessingService {

    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_VIDEO_DURATION_SECONDS = 20; // 20 seconds

    /**
     * Validate video file
     * Note: Duration validation should be done on client side (Flutter) before upload
     * This method only validates file type and size
     */
    public void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Video file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("File must be a video");
        }

        // Check file size (max 50MB)
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new IllegalArgumentException("Video size must be less than 50MB");
        }

        // Check allowed video types
        String[] allowedTypes = {
            "video/mp4", 
            "video/quicktime", // MOV
            "video/x-msvideo", // AVI
            "video/webm"
        };
        boolean isAllowed = false;
        for (String type : allowedTypes) {
            if (contentType.equals(type)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            throw new IllegalArgumentException("Video type must be MP4, MOV, AVI, or WebM");
        }
    }

    /**
     * Get max video duration in seconds
     */
    public int getMaxVideoDurationSeconds() {
        return MAX_VIDEO_DURATION_SECONDS;
    }
}
