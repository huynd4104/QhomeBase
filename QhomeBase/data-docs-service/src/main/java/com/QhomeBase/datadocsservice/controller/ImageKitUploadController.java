package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.service.ImageKitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/imagekit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ImageKit Upload", description = "APIs for uploading images/files to ImageKit")
public class ImageKitUploadController {

    private final ImageKitService imageKitService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload single image/file to ImageKit", description = "Upload a single image or file to ImageKit")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        
        try {
            log.info("📤 [ImageKitUpload] Uploading file: {} (size: {} bytes) to folder: {}", 
                    file.getOriginalFilename(), file.getSize(), folder);
            
            // Validate file size (max 50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                log.error("❌ [ImageKitUpload] File too large: {} bytes", file.getSize());
                Map<String, String> error = new HashMap<>();
                error.put("error", "File size exceeds maximum limit of 50MB");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            String imageUrl = imageKitService.uploadImage(file, folder);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", imageUrl);
            response.put("fileName", file.getOriginalFilename());
            
            log.info("✅ [ImageKitUpload] File uploaded successfully: {}", imageUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ [ImageKitUpload] Invalid argument: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IOException e) {
            log.error("❌ [ImageKitUpload] IO error uploading file: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            log.error("❌ [ImageKitUpload] Unexpected error uploading file: {}", e.getMessage(), e);
            log.error("❌ [ImageKitUpload] Exception type: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("❌ [ImageKitUpload] Caused by: {}", e.getCause().getMessage());
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/upload-multiple")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload multiple images/files to ImageKit", description = "Upload multiple images or files to ImageKit")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "folder", required = false) String folder) {
        
        try {
            log.info("📤 [ImageKitUpload] Uploading {} files to folder: {}", files.length, folder);
            
            // Validate files
            if (files == null || files.length == 0) {
                log.error("❌ [ImageKitUpload] No files provided");
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No files provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Validate file sizes
            for (MultipartFile file : files) {
                if (file.getSize() > 50 * 1024 * 1024) {
                    log.error("❌ [ImageKitUpload] File too large: {} bytes", file.getSize());
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "File " + file.getOriginalFilename() + " exceeds maximum limit of 50MB");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }
            
            List<String> imageUrls = imageKitService.uploadImages(List.of(files), folder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("urls", imageUrls);
            response.put("count", imageUrls.size());
            
            log.info("✅ [ImageKitUpload] Uploaded {} files successfully", imageUrls.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ [ImageKitUpload] Invalid argument: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (IOException e) {
            log.error("❌ [ImageKitUpload] IO error uploading files: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            log.error("❌ [ImageKitUpload] Unexpected error uploading files: {}", e.getMessage(), e);
            log.error("❌ [ImageKitUpload] Exception type: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("❌ [ImageKitUpload] Caused by: {}", e.getCause().getMessage());
            }
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
