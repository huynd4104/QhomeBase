package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.dto.VideoUploadResponse;
import com.QhomeBase.datadocsservice.exception.FileStorageException;
import com.QhomeBase.datadocsservice.model.VideoStorage;
import com.QhomeBase.datadocsservice.service.VideoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Video Upload", description = "Video upload and management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class VideoUploadController {

    private final VideoStorageService videoStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload video", description = "Upload a video file to backend storage (NOT ImageKit - videos are self-hosted). Videos are stored in filesystem and metadata in database.")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "ownerId", required = false) UUID ownerId,
            @RequestParam("uploadedBy") UUID uploadedBy,
            @RequestParam(value = "resolution", required = false) String resolution,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam(value = "width", required = false) Integer width,
            @RequestParam(value = "height", required = false) Integer height,
            HttpServletRequest request) {
        
        try {
            log.info("🔍 [VideoUploadController] Received upload request: category={}, ownerId={}, uploadedBy={}, fileName={}, size={} MB",
                    category, ownerId, uploadedBy, file.getOriginalFilename(), file.getSize() / (1024.0 * 1024.0));
            
            // Store relative path - Flutter app will prepend base URL from app_config.dart
            // This allows easy URL updates without database changes
            VideoStorage videoStorage = videoStorageService.uploadVideo(
                    file, category, ownerId, uploadedBy, resolution, durationSeconds, width, height, null);
            
            // Only log essential info - no spam logging
            if (log.isDebugEnabled()) {
                log.debug("✅ [VideoUploadController] Video uploaded: videoId={}", videoStorage.getId());
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VideoUploadResponse.from(videoStorage));
        } catch (FileStorageException e) {
            log.error("❌ [VideoUploadController] FileStorageException: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ [VideoUploadController] Unexpected error uploading video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    @GetMapping("/{videoId}")
    @Operation(summary = "Get video metadata", description = "Get video metadata by ID. Returns relative path - client should prepend base URL.")
    public ResponseEntity<VideoUploadResponse> getVideo(@PathVariable UUID videoId) {
        try {
            VideoStorage video = videoStorageService.getVideoById(videoId);
            // Return relative path - Flutter app will prepend base URL from app_config.dart
            // If stored URL is full URL, normalize to relative path
            String videoUrl = normalizeToRelativePath(video.getFileUrl(), video.getId());
            VideoUploadResponse response = VideoUploadResponse.from(video);
            VideoUploadResponse responseWithRelativeUrl = new VideoUploadResponse(
                    response.id(),
                    response.fileName(),
                    response.originalFileName(),
                    videoUrl, // Use relative path
                    response.contentType(),
                    response.fileSize(),
                    response.category(),
                    response.ownerId(),
                    response.resolution(),
                    response.durationSeconds(),
                    response.width(),
                    response.height(),
                    response.uploadedBy()
            );
            return ResponseEntity.ok(responseWithRelativeUrl);
        } catch (FileStorageException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Normalize video URL to relative path
     * If URL is full URL (contains http:// or https://), extract relative path
     * Otherwise return as-is (already relative)
     */
    private String normalizeToRelativePath(String fileUrl, UUID videoId) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return String.format("/api/videos/stream/%s", videoId.toString());
        }
        
        // If URL contains /api/videos/stream/, extract relative path
        if (fileUrl.contains("/api/videos/stream/")) {
            String[] parts = fileUrl.split("/api/videos/stream/");
            if (parts.length > 1) {
                String videoIdStr = parts[1].split("\\?")[0]; // Remove query params if any
                // Validate that videoIdStr matches the actual videoId
                if (videoIdStr.equals(videoId.toString())) {
                    return "/api/videos/stream/" + videoIdStr;
                }
            }
        }
        
        // If it's a full URL (starts with http:// or https://), extract path
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            try {
                java.net.URL url = new java.net.URL(fileUrl);
                String path = url.getPath();
                if (path != null && !path.isEmpty() && path.contains("/api/videos/stream/")) {
                    // Extract relative path from full URL
                    String[] parts = path.split("/api/videos/stream/");
                    if (parts.length > 1) {
                        String videoIdStr = parts[1].split("\\?")[0];
                        return "/api/videos/stream/" + videoIdStr;
                    }
                }
            } catch (Exception e) {
                // If URL parsing fails, fall through to use videoId
            }
        }
        
        // If already relative path (starts with /), return as-is
        if (fileUrl.startsWith("/")) {
            return fileUrl;
        }
        
        // Fallback: return relative path with videoId
        return String.format("/api/videos/stream/%s", videoId.toString());
    }

    @GetMapping("/category/{category}/owner/{ownerId}")
    @Operation(summary = "Get videos by category and owner", description = "Get all videos for a specific category and owner. Returns relative paths - client should prepend base URL.")
    public ResponseEntity<List<VideoUploadResponse>> getVideosByCategoryAndOwner(
            @PathVariable String category,
            @PathVariable UUID ownerId) {
        try {
            List<VideoStorage> videos = videoStorageService.getVideosByCategoryAndOwner(category, ownerId);
            List<VideoUploadResponse> responses = videos.stream()
                    .map(video -> {
                        // Normalize to relative path - Flutter app will prepend base URL from app_config.dart
                        String relativeUrl = normalizeToRelativePath(video.getFileUrl(), video.getId());
                        VideoUploadResponse original = VideoUploadResponse.from(video);
                        return new VideoUploadResponse(
                                original.id(),
                                original.fileName(),
                                original.originalFileName(),
                                relativeUrl, // Use relative path
                                original.contentType(),
                                original.fileSize(),
                                original.category(),
                                original.ownerId(),
                                original.resolution(),
                                original.durationSeconds(),
                                original.width(),
                                original.height(),
                                original.uploadedBy()
                        );
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error getting videos", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/stream/{videoId}", method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS})
    @Operation(summary = "Stream video", description = "Stream video file by ID - Public access for video playback")
    public ResponseEntity<?> streamVideo(
            @PathVariable UUID videoId,
            HttpServletRequest request) {
        
        // Handle CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600")
                    .build();
        }
        
        // Handle HEAD requests
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            try {
                VideoStorage video = videoStorageService.getVideoById(videoId);
                Path videoPath = Paths.get(video.getFilePath());
                Resource resource = new UrlResource(videoPath.toUri());
                
                if (!resource.exists() || !resource.isReadable()) {
                    return ResponseEntity.notFound().build();
                }
                
                String contentType = video.getContentType();
                if (contentType == null) {
                    contentType = "video/mp4";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(video.getFileSize()))
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*")
                        .build();
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }
        try {
            // Only log errors - no spam logging for every stream request
            // Video streaming is high-frequency, don't log every request
            
            VideoStorage video = videoStorageService.getVideoById(videoId);
            
            Path videoPath = Paths.get(video.getFilePath());
            Resource resource = new UrlResource(videoPath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("⚠️ [VideoUploadController] Video file not found or not readable: {}", video.getFilePath());
                return ResponseEntity.notFound().build();
            }
            
            String contentType = video.getContentType();
            if (contentType == null) {
                contentType = "video/mp4";
            }
            
            // Support range requests for video streaming
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getOriginalFileName() + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    // CORS headers for ExoPlayer
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS")
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
            
            // Handle range requests for video streaming
            // HTTP Range requests are critical for video seeking - player requests specific byte ranges
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    long fileSize = resource.contentLength();
                    if (fileSize <= 0) {
                        // File size unknown, return full resource
                        return responseBuilder.body(resource);
                    }
                    
                    String[] ranges = rangeHeader.replace("bytes=", "").split("-");
                    long start = Long.parseLong(ranges[0]);
                    long end = ranges.length > 1 && !ranges[1].isEmpty() 
                            ? Long.parseLong(ranges[1]) 
                            : fileSize - 1;
                    
                    // Validate range
                    if (start > end || start < 0 || end >= fileSize) {
                        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                                .build();
                    }
                    
                    long contentLength = end - start + 1;
                    
                    // Return partial content response (206 Partial Content)
                    // This allows video player to seek to any position without downloading entire file
                    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.getOriginalFileName() + "\"")
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, HEAD, OPTIONS")
                            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "*")
                            .header(HttpHeaders.CONTENT_RANGE, 
                                    String.format("bytes %d-%d/%d", start, end, fileSize))
                            .contentLength(contentLength)
                            .body(resource);
                } catch (Exception e) {
                    log.warn("⚠️ [VideoUploadController] Invalid range header: {}", rangeHeader);
                    // Fall through to return full resource if range parsing fails
                }
            }
            
            // No range header - return full resource with proper headers for streaming
            // Spring Boot will handle Range requests automatically if ACCEPT_RANGES is set
            long fileSize = resource.contentLength();
            return responseBuilder
                    .contentLength(fileSize > 0 ? fileSize : -1)
                    .body(resource);
        } catch (FileStorageException e) {
            log.error("❌ [VideoUploadController] Video not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("❌ [VideoUploadController] Error streaming video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{videoId}")
    @Operation(summary = "Delete video", description = "Soft delete a video by ID")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID videoId) {
        try {
            videoStorageService.deleteVideo(videoId);
            return ResponseEntity.noContent().build();
        } catch (FileStorageException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
