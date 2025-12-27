package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.dto.FileUploadResponse;
import com.QhomeBase.datadocsservice.exception.FileNotFoundException;
import com.QhomeBase.datadocsservice.exception.FileStorageException;
import com.QhomeBase.datadocsservice.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "File upload and management APIs")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/news-image")
    @Operation(summary = "Upload news image", description = "Upload a single image for news article")
    public ResponseEntity<FileUploadResponse> uploadNewsImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerId") UUID ownerId,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        log.info("Uploading news image: {} for owner: {}", file.getOriginalFilename(), ownerId);
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        FileUploadResponse response = fileStorageService.uploadImage(
                file, 
                ownerId, 
                uploadedBy, 
                "news"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload/news-images")
    @Operation(summary = "Upload multiple news images", description = "Upload multiple images for news article")
    public ResponseEntity<List<FileUploadResponse>> uploadNewsImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("ownerId") UUID ownerId,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        log.info("Uploading {} news images for owner: {}", files.length, ownerId);
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            FileUploadResponse response = fileStorageService.uploadImage(
                    file, 
                    ownerId, 
                    uploadedBy, 
                    "news"
            );
            responses.add(response);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @PostMapping("/upload/profile-image")
    @Operation(summary = "Upload profile image", description = "Upload profile/avatar image")
    public ResponseEntity<FileUploadResponse> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerId") UUID ownerId,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        log.info("Uploading profile image: {} for owner: {}", file.getOriginalFilename(), ownerId);
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        FileUploadResponse response = fileStorageService.uploadImage(
                file, 
                ownerId, 
                uploadedBy, 
                "profile"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{category}/{ownerId}/{date}/{fileName:.+}")
    @Operation(summary = "Get file", description = "Download or view uploaded file")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String category,
            @PathVariable String ownerId,
            @PathVariable String date,
            @PathVariable String fileName,
            HttpServletRequest request) {
        
        Resource resource = fileStorageService.loadFileAsResource(category, ownerId, date, fileName);
        
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

    @GetMapping("/contracts/{contractId}/{fileName:.+}")
    @Operation(summary = "Get contract file", description = "Download or view contract file")
    public ResponseEntity<Resource> getContractFile(
            @PathVariable UUID contractId,
            @PathVariable String fileName,
            HttpServletRequest request) {
        
        Resource resource = fileStorageService.loadContractFileAsResource(contractId, fileName);
        
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


    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFoundException(FileNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error during file upload", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                        "An error occurred while processing your request: " + ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}

