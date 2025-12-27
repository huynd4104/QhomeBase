package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.dto.CategoryResponse;
import com.QhomeBase.marketplaceservice.mapper.MarketplaceMapper;
import com.QhomeBase.marketplaceservice.service.MarketplaceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Marketplace Categories", description = "APIs for managing marketplace categories")
public class MarketplaceCategoryController {

    private final MarketplaceCategoryService categoryService;
    private final MarketplaceMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('RESIDENT')")
    @Operation(summary = "Get all categories", description = "Get all active marketplace categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = categoryService.getAllActiveCategories().stream()
                .map(mapper::toCategoryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }
}

