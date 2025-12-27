package com.QhomeBase.customerinteractionservice.controller;

import com.QhomeBase.customerinteractionservice.dto.news.*;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import com.QhomeBase.customerinteractionservice.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @PostMapping
    @PreAuthorize("@authz.canCreateNews()")
    public ResponseEntity<NewsManagementResponse> createNews(
            @Valid @RequestBody CreateNewsRequest request,
            Authentication authentication) {
        
        NewsManagementResponse response = newsService.createNews(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.canUpdateNews()")
    public ResponseEntity<NewsManagementResponse> updateNews(
            @PathVariable("id") UUID newsId,
            @Valid @RequestBody UpdateNewsRequest request,
            Authentication authentication) {
        
        NewsManagementResponse response = newsService.updateNews(newsId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.canDeleteNews()")
    public ResponseEntity<NewsManagementResponse> deleteNews(
            @PathVariable("id") UUID newsId,
            Authentication authentication) {
        
        var principal = (UserPrincipal) authentication.getPrincipal();
        NewsManagementResponse response = newsService.deleteNews(newsId, principal.uid());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    // @PreAuthorize("@authz.canViewNews()")
    public ResponseEntity<List<NewsManagementResponse>> getAllNews() {
        
        List<NewsManagementResponse> news = newsService.getAllNews();
        return ResponseEntity.ok(news);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewNews()")
    public ResponseEntity<NewsManagementResponse> getNewsDetail(
            @PathVariable("id") UUID newsId) {
        
        NewsManagementResponse response = newsService.getNewsDetail(newsId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/resident")
    public ResponseEntity<?> getNewsForResident(
            @RequestParam UUID residentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size) {
        
        // Ensure size is 7 as per requirement
        size = 7;
        
        log.info("🔍 [NewsController] getNewsForResident: residentId={}, page={}, size={}", 
                residentId, page, size);
        
        NewsPagedResponse news = newsService.getNewsForResidentPaged(residentId, page, size);
        
        log.info("✅ [NewsController] getNewsForResident: returned {} news items", 
                news.getContent().size());
        
        return ResponseEntity.ok(news);
    }

    @GetMapping("/{id}/resident")
    public ResponseEntity<NewsDetailResponse> getNewsDetailForResident(
            @PathVariable("id") UUID newsId,
            @RequestParam UUID residentId) {
        
        NewsDetailResponse response = newsService.getNewsForResident(newsId, residentId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error: " + ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}

