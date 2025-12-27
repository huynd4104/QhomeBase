package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.news.NewsImageDto;
import com.QhomeBase.customerinteractionservice.dto.news.UpdateImageRequest;
import com.QhomeBase.customerinteractionservice.dto.news.UploadImageResponse;
import com.QhomeBase.customerinteractionservice.security.AuthzService;
import com.QhomeBase.customerinteractionservice.service.NewsImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/news/images")
@RequiredArgsConstructor
public class NewsImageController {

    private final NewsImageUploadService newsImageUploadService;
    private final AuthzService authzService;

    @PostMapping
    @PreAuthorize("@authz.canUploadNewsImage()")
    public ResponseEntity<UploadImageResponse> uploadImage(
            @Valid @RequestBody NewsImageDto newsImageDto) {
        
        UploadImageResponse response = newsImageUploadService.uploadImage(newsImageDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/batch")
    @PreAuthorize("@authz.canUploadNewsImage()")
    public ResponseEntity<List<UploadImageResponse>> uploadMultipleImages(
            @Valid @RequestBody List<NewsImageDto> imageDtos) {
        
        List<UploadImageResponse> responses = newsImageUploadService.uploadMultipleImages(imageDtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/news/{newsId}")
    public ResponseEntity<List<UploadImageResponse>> getImagesByNewsId(
            @PathVariable UUID newsId) {
        
        List<UploadImageResponse> images = newsImageUploadService.getImagesByNewsId(newsId);
        return ResponseEntity.ok(images);
    }

    @PutMapping("/{imageId}")
    @PreAuthorize("@authz.canUpdateNewsImage()")
    public ResponseEntity<UploadImageResponse> updateImage(
            @PathVariable UUID imageId,
            @Valid @RequestBody UpdateImageRequest request) {
        
        UploadImageResponse response;
        if (request.getSortOrder() != null) {
            response = newsImageUploadService.updateSortOrder(imageId, request.getSortOrder());
        } else if (request.getCaption() != null) {
            response = newsImageUploadService.updateImageCaption(imageId, request.getCaption());
        } else {
            throw new IllegalArgumentException("Either caption or sortOrder must be provided");
        }
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{imageId}/caption")
    @PreAuthorize("@authz.canUpdateNewsImage()")
    public ResponseEntity<UploadImageResponse> updateCaption(
            @PathVariable UUID imageId,
            @RequestParam String caption) {
        
        UploadImageResponse response = newsImageUploadService.updateImageCaption(imageId, caption);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{imageId}/sort-order")
    @PreAuthorize("@authz.canUpdateNewsImage()")
    public ResponseEntity<UploadImageResponse> updateSortOrder(
            @PathVariable UUID imageId,
            @RequestParam Integer sortOrder) {
        
        UploadImageResponse response = newsImageUploadService.updateSortOrder(imageId, sortOrder);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("@authz.canDeleteNewsImage()")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID imageId) {
        newsImageUploadService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}































