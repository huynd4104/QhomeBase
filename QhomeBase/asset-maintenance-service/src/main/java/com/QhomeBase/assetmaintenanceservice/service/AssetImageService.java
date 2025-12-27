package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.dto.asset.AssetResponse;
import com.QhomeBase.assetmaintenanceservice.model.Asset;
import com.QhomeBase.assetmaintenanceservice.repository.AssetRepository;
import com.QhomeBase.assetmaintenanceservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AssetImageService {

    private final AssetRepository assetRepository;
    private final FileStorageService fileStorageService;
    private final AssetService assetService;

    public AssetResponse uploadImages(UUID assetId, List<MultipartFile> files, Authentication authentication) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));

        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot upload images to deleted asset");
        }

        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();

        List<String> uploadedPaths = fileStorageService.uploadMultipleAssetImages(files, assetId);

        List<String> currentImageUrls = asset.getImageUrls() != null ? 
                new ArrayList<>(asset.getImageUrls()) : new ArrayList<>();
        currentImageUrls.addAll(uploadedPaths);

        asset.setImageUrls(currentImageUrls);
        asset.setUpdatedBy(userId.toString());
        asset.setUpdatedAt(Instant.now());

        Asset updatedAsset = assetRepository.save(asset);
        return assetService.toDto(updatedAsset);
    }

    public AssetResponse deleteImage(UUID assetId, String imageUrl, Authentication authentication) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));

        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot delete images from deleted asset");
        }

        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();

        List<String> currentImageUrls = asset.getImageUrls() != null ? 
                new ArrayList<>(asset.getImageUrls()) : new ArrayList<>();

        if (!currentImageUrls.contains(imageUrl)) {
            throw new IllegalArgumentException("Image URL not found in asset images");
        }

        try {
            fileStorageService.deleteAssetImage(imageUrl, assetId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image file: " + e.getMessage());
        }

        currentImageUrls.remove(imageUrl);
        asset.setImageUrls(currentImageUrls);
        asset.setUpdatedBy(userId.toString());
        asset.setUpdatedAt(Instant.now());

        Asset updatedAsset = assetRepository.save(asset);
        return assetService.toDto(updatedAsset);
    }

    public AssetResponse setPrimaryImage(UUID assetId, String imageUrl, Authentication authentication) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + assetId));

        if (asset.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot set primary image for deleted asset");
        }

        var p = (UserPrincipal) authentication.getPrincipal();
        UUID userId = p.uid();

        List<String> currentImageUrls = asset.getImageUrls() != null ? 
                new ArrayList<>(asset.getImageUrls()) : new ArrayList<>();

        if (!currentImageUrls.contains(imageUrl)) {
            throw new IllegalArgumentException("Image URL not found in asset images");
        }

        currentImageUrls.remove(imageUrl);
        currentImageUrls.add(0, imageUrl);

        asset.setImageUrls(currentImageUrls);
        asset.setUpdatedBy(userId.toString());
        asset.setUpdatedAt(Instant.now());

        Asset updatedAsset = assetRepository.save(asset);
        return assetService.toDto(updatedAsset);
    }
}










