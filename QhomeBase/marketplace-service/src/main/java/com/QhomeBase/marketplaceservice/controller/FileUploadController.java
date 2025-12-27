package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.service.FileStorageService;
import com.QhomeBase.marketplaceservice.service.ImageProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "File Uploads", description = "APIs for serving uploaded images")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final ImageProcessingService imageProcessingService;

    @GetMapping("/{postId}/{fileName:.+}")
    @Operation(summary = "Get image", description = "Get uploaded image file (public access for marketplace posts)")
    public ResponseEntity<Resource> getImage(
            @PathVariable String postId,
            @PathVariable String fileName) {
        
        log.info("📸 [FileUploadController] Request to get image: postId={}, fileName={}", postId, fileName);
        
        try {
            Path filePath = fileStorageService.getImagePath(postId, fileName);
            log.info("📸 [FileUploadController] Image path: {}", filePath);
            log.info("📸 [FileUploadController] File exists: {}", java.nio.file.Files.exists(filePath));
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                log.info("✅ [FileUploadController] Serving image: postId={}, fileName={}, size={}", 
                        postId, fileName, resource.contentLength());
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // Cache for 1 year
                        .body(resource);
            } else {
                log.warn("⚠️ [FileUploadController] Image not found or not readable: postId={}, fileName={}, path={}", 
                        postId, fileName, filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ [FileUploadController] Error serving image: postId={}, fileName={}", postId, fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/marketplace/comment/image")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload comment image", description = "Upload an image for a marketplace comment")
    public ResponseEntity<Map<String, String>> uploadCommentImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("postId") String postId) {
        
        log.info("📸 [FileUploadController] Uploading comment image for post: {}", postId);
        
        try {
            // Validate image
            imageProcessingService.validateImage(file);
            
            // Upload image
            String imageUrl = fileStorageService.uploadCommentImage(file, postId);
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            
            log.info("✅ [FileUploadController] Comment image uploaded successfully: {}", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ [FileUploadController] Invalid image: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ [FileUploadController] Error uploading comment image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/marketplace/comment/video")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload comment video", description = "Upload a video for a marketplace comment")
    public ResponseEntity<Map<String, String>> uploadCommentVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("postId") String postId) {
        
        log.info("🎥 [FileUploadController] Uploading comment video for post: {}", postId);
        
        try {
            // Validate video (basic check - file size)
            if (file.getSize() > 100 * 1024 * 1024) { // 100MB limit
                log.error("❌ [FileUploadController] Video file too large: {} bytes", file.getSize());
                return ResponseEntity.badRequest().build();
            }
            
            // Upload video
            String videoUrl = fileStorageService.uploadCommentVideo(file, postId);
            
            Map<String, String> response = new HashMap<>();
            response.put("videoUrl", videoUrl);
            
            log.info("✅ [FileUploadController] Comment video uploaded successfully: {}", videoUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [FileUploadController] Error uploading comment video: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{postId}/comments/{fileName:.+}")
    @Operation(summary = "Get comment image/video", description = "Get uploaded comment image or video file (public access)")
    public ResponseEntity<Resource> getCommentFile(
            @PathVariable String postId,
            @PathVariable String fileName) {
        
        log.info("📸 [FileUploadController] Request to get comment file: postId={}, fileName={}", postId, fileName);
        
        try {
            Path filePath = fileStorageService.getCommentFilePath(postId, fileName);
            log.info("📸 [FileUploadController] Comment file path: {}", filePath);
            log.info("📸 [FileUploadController] File exists: {}", java.nio.file.Files.exists(filePath));
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type based on file extension
                MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
                String lowerFileName = fileName.toLowerCase();
                if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                    mediaType = MediaType.IMAGE_JPEG;
                } else if (lowerFileName.endsWith(".png")) {
                    mediaType = MediaType.IMAGE_PNG;
                } else if (lowerFileName.endsWith(".mp4")) {
                    mediaType = MediaType.parseMediaType("video/mp4");
                } else if (lowerFileName.endsWith(".webm")) {
                    mediaType = MediaType.parseMediaType("video/webm");
                }
                
                log.info("✅ [FileUploadController] Serving comment file: postId={}, fileName={}, size={}", 
                        postId, fileName, resource.contentLength());
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000") // Cache for 1 year
                        .body(resource);
            } else {
                log.warn("⚠️ [FileUploadController] Comment file not found or not readable: postId={}, fileName={}, path={}", 
                        postId, fileName, filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ [FileUploadController] Error serving comment file: postId={}, fileName={}", postId, fileName, e);
            return ResponseEntity.notFound().build();
        }
    }
}

