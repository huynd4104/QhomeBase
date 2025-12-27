package com.QhomeBase.chatservice.controller;

import com.QhomeBase.chatservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/uploads/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "File upload APIs for chat")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    // Direct Chat File Upload Endpoints
    @PostMapping("/direct/{conversationId}/image")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload image for direct chat", description = "Upload an image file for a direct chat message")
    public ResponseEntity<Map<String, String>> uploadDirectImage(
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Use conversationId as folder identifier (similar to groupId)
            String imageUrl = fileStorageService.uploadImage(file, conversationId);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("mimeType", contentType);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading direct chat image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image"));
        }
    }

    @PostMapping("/direct/{conversationId}/images")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload multiple images for direct chat", description = "Upload multiple image files for direct chat messages")
    public ResponseEntity<Map<String, Object>> uploadDirectImages(
            @PathVariable UUID conversationId,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "No files provided"));
            }

            List<String> imageUrls = new ArrayList<>();
            List<String> mimeTypes = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    continue; // Skip non-image files
                }

                String imageUrl = fileStorageService.uploadImage(file, conversationId);
                imageUrls.add(imageUrl);
                mimeTypes.add(contentType);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("imageUrls", imageUrls);
            response.put("mimeTypes", mimeTypes);
            response.put("count", imageUrls.size());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading direct chat images: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload images"));
        }
    }

    @PostMapping("/direct/{conversationId}/audio")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload audio for direct chat", description = "Upload an audio file for a direct chat message")
    public ResponseEntity<Map<String, String>> uploadDirectAudio(
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an audio file"));
            }

            String audioUrl = fileStorageService.uploadAudio(file, conversationId);

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", audioUrl);
            response.put("mimeType", contentType);
            response.put("fileSize", String.valueOf(file.getSize()));
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading direct chat audio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload audio"));
        }
    }

    @PostMapping("/direct/{conversationId}/video")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload video for direct chat", description = "Upload a video file for a direct chat message")
    public ResponseEntity<Map<String, String>> uploadDirectVideo(
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be a video file"));
            }

            String videoUrl = fileStorageService.uploadVideo(file, conversationId);

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", videoUrl);
            response.put("fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.mp4");
            response.put("fileSize", String.valueOf(file.getSize()));
            response.put("mimeType", contentType);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading direct chat video: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload video"));
        }
    }

    @PostMapping("/direct/{conversationId}/file")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload file for direct chat", description = "Upload any file for a direct chat message")
    public ResponseEntity<Map<String, String>> uploadDirectFile(
            @PathVariable UUID conversationId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String fileUrl = fileStorageService.uploadFile(file, conversationId);
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", String.valueOf(file.getSize()));
            response.put("mimeType", contentType);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading direct chat file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }

    @PostMapping("/{groupId}/image")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload image for chat message", description = "Upload an image file for a chat message")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Upload image
            String imageUrl = fileStorageService.uploadImage(file, groupId);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload image"));
        }
    }

    @PostMapping("/{groupId}/images")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload multiple images for chat messages", description = "Upload multiple image files for chat messages. Each image will be sent as a separate message.")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable UUID groupId,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            // Validate files
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one file is required"));
            }

            // Validate all files are images
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "One or more files are empty"));
                }
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(Map.of("error", "All files must be images"));
                }
            }

            // Upload images
            List<String> imageUrls = fileStorageService.uploadImages(List.of(files), groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("imageUrls", imageUrls);
            response.put("count", imageUrls.size());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading images: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload images"));
        }
    }

    @PostMapping("/{groupId}/audio")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload audio for chat message", description = "Upload an audio file (voice message) for a chat message")
    public ResponseEntity<Map<String, String>> uploadAudio(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an audio file"));
            }

            // Upload audio
            String audioUrl = fileStorageService.uploadAudio(file, groupId);

            Map<String, String> response = new HashMap<>();
            response.put("audioUrl", audioUrl);
            response.put("fileSize", String.valueOf(file.getSize()));
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading audio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload audio"));
        }
    }

    @PostMapping("/{groupId}/video")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload video for chat message", description = "Upload a video file for a chat message")
    public ResponseEntity<Map<String, String>> uploadVideo(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("video/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be a video file"));
            }

            // Validate file size (max 50MB)
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 50MB limit"));
            }

            // Upload video
            String videoUrl = fileStorageService.uploadVideo(file, groupId);
            String fileName = file.getOriginalFilename();

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", videoUrl);
            response.put("fileName", fileName != null ? fileName : "video.mp4");
            response.put("fileSize", String.valueOf(file.getSize()));
            response.put("mimeType", contentType);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading video: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload video"));
        }
    }

    @PostMapping("/{groupId}/file")
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Upload file for chat message", description = "Upload a file (document, PDF, zip, etc.) for a chat message")
    public ResponseEntity<Map<String, String>> uploadFile(
            @PathVariable UUID groupId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file size (max 50MB)
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 50MB limit"));
            }

            // Upload file
            String fileUrl = fileStorageService.uploadFile(file, groupId);
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();

            Map<String, String> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", fileName != null ? fileName : "file");
            response.put("fileSize", String.valueOf(file.getSize()));
            response.put("mimeType", contentType != null ? contentType : "application/octet-stream");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file"));
        }
    }
}

