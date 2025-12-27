package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.exception.FileNotFoundException;
import com.QhomeBase.datadocsservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Controller to serve files by simple path (for backward compatibility)
 * Handles URLs like: /files/news/cover_maintenance.jpg
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Serve", description = "File serving APIs for backward compatibility")
public class FileServeController {

    private final FileStorageService fileStorageService;

    /**
     * Serve files by simple path (for backward compatibility with old URL format)
     * Example: /files/news/cover_maintenance.jpg
     * Maps to: uploads/news/cover_maintenance.jpg in storage
     */
    @GetMapping("/**")
    @Operation(summary = "Get file by path", description = "Download or view file by simple path (for backward compatibility)")
    public ResponseEntity<Resource> getFileByPath(
            HttpServletRequest request) {
        
        // Extract path after /files/ (remove /files prefix)
        String requestPath = request.getRequestURI();
        String pathPrefix = "/files/";
        
        if (!requestPath.startsWith(pathPrefix)) {
            throw new FileNotFoundException("Invalid file path format. Expected path starting with: " + pathPrefix);
        }
        
        // Extract file path: /files/news/cover_maintenance.jpg -> news/cover_maintenance.jpg
        String filePath = requestPath.substring(pathPrefix.length());
        
        log.debug("Serving file by path: {} (from request: {})", filePath, requestPath);
        
        Resource resource = fileStorageService.loadFileByPath(filePath);
        
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<?> handleFileNotFoundException(FileNotFoundException ex, HttpServletRequest request) {
        log.warn("File not found: {} | Request: {}", ex.getMessage(), request.getRequestURI());
        
        // If client accepts JSON, return JSON error
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return ResponseEntity
                    .status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .contentType(MediaType.valueOf("application/json"))
                    .body(new ErrorResponse(org.springframework.http.HttpStatus.NOT_FOUND.value(), ex.getMessage()));
        }
        
        // Otherwise, return plain text (for image requests)
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.NOT_FOUND)
                .contentType(MediaType.valueOf("text/plain"))
                .body("File not found: " + ex.getMessage());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Error serving file: {} | Request: {}", ex.getMessage(), request.getRequestURI(), ex);
        
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            return ResponseEntity
                    .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.valueOf("application/json"))
                    .body(new ErrorResponse(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                            "Internal server error: " + ex.getMessage()));
        }
        
        return ResponseEntity
                .status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.valueOf("text/plain"))
                .body("Internal server error: " + ex.getMessage());
    }

    record ErrorResponse(int status, String message) {}
}

