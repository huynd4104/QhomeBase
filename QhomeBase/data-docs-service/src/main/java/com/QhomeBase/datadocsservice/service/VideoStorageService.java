package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.config.FileStorageProperties;
import com.QhomeBase.datadocsservice.exception.FileStorageException;
import com.QhomeBase.datadocsservice.model.VideoStorage;
import com.QhomeBase.datadocsservice.repository.VideoStorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class VideoStorageService {

    private final Path fileStorageLocation;
    private final FileStorageProperties fileStorageProperties;
    private final VideoStorageRepository videoStorageRepository;
    
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4",
            "video/quicktime", // .mov
            "video/x-msvideo", // .avi
            "video/webm"
    );
    
    // Giới hạn 50MB cho video (sau khi nén)
    private static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024; // 50MB

    public VideoStorageService(FileStorageProperties fileStorageProperties, VideoStorageRepository videoStorageRepository) {
        this.fileStorageProperties = fileStorageProperties;
        this.videoStorageRepository = videoStorageRepository;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getLocation())
                .toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation.resolve("videos"));
            log.info("Video storage location initialized at: {}", this.fileStorageLocation.resolve("videos"));
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded videos will be stored.", ex);
        }
    }

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Video file is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("Invalid video file type. Allowed types: " + ALLOWED_VIDEO_TYPES);
        }

        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new FileStorageException(
                    String.format("Video file too large. Maximum size: %d MB, actual size: %.2f MB",
                            MAX_VIDEO_SIZE / (1024 * 1024),
                            file.getSize() / (1024.0 * 1024.0))
            );
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".mp4";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    @Transactional
    public VideoStorage uploadVideo(
            MultipartFile file,
            String category,
            UUID ownerId,
            UUID uploadedBy,
            String resolution,
            Integer durationSeconds,
            Integer width,
            Integer height,
            String gatewayBaseUrl) { // Not used anymore - we store relative path
        
        validateVideoFile(file);
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            originalFileName = "video.mp4";
        }
        originalFileName = StringUtils.cleanPath(originalFileName);
        String fileExtension = getFileExtension(originalFileName);
        
        String fileName = UUID.randomUUID().toString() + fileExtension;
        
        String datePath = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        Path targetLocation = this.fileStorageLocation
                .resolve("videos")
                .resolve(category)
                .resolve(ownerId != null ? ownerId.toString() : "general")
                .resolve(datePath)
                .resolve(fileName);
        
        try {
            Files.createDirectories(targetLocation.getParent());
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            // Create video storage first to get ID, then update with correct URL pointing to stream endpoint
            VideoStorage videoStorage = VideoStorage.builder()
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .filePath(targetLocation.toString())
                    .fileUrl("") // Temporary, will be updated after save
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .category(category)
                    .ownerId(ownerId)
                    .resolution(resolution)
                    .durationSeconds(durationSeconds)
                    .width(width)
                    .height(height)
                    .uploadedBy(uploadedBy)
                    .isDeleted(false)
                    .build();
            
            VideoStorage saved = videoStorageRepository.save(videoStorage);
            
            // Store relative path instead of full URL to avoid hardcoded IP issues
            // Client (Flutter app) will prepend base URL from app_config.dart
            // This allows easy URL updates without database changes
            String fileUrl = String.format("/api/videos/stream/%s", saved.getId().toString());
            saved.setFileUrl(fileUrl);
            saved = videoStorageRepository.save(saved);
            
            // Only log essential info - no spam logging
            if (log.isDebugEnabled()) {
                log.debug("Video uploaded: videoId={}, category={}, size={} MB, url={}", 
                        saved.getId(), category, file.getSize() / (1024.0 * 1024.0), fileUrl);
            }
            
            return saved;
            
        } catch (IOException ex) {
            throw new FileStorageException("Could not store video file " + fileName + ". Please try again!", ex);
        }
    }

    public VideoStorage getVideoById(UUID videoId) {
        return videoStorageRepository.findByIdAndIsDeletedFalse(videoId)
                .orElseThrow(() -> new FileStorageException("Video not found with ID: " + videoId));
    }

    public List<VideoStorage> getVideosByCategoryAndOwner(String category, UUID ownerId) {
        return videoStorageRepository.findActiveVideosByCategoryAndOwner(category, ownerId);
    }

    @Transactional
    public void deleteVideo(UUID videoId) {
        VideoStorage video = getVideoById(videoId);
        video.setIsDeleted(true);
        video.setDeletedAt(java.time.OffsetDateTime.now());
        videoStorageRepository.save(video);
        log.info("Video marked as deleted: {}", videoId);
    }
}
