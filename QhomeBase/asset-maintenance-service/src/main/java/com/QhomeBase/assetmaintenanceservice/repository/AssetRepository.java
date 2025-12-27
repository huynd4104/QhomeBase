package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.Asset;
import com.QhomeBase.assetmaintenanceservice.model.AssetStatus;
import com.QhomeBase.assetmaintenanceservice.model.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    
    Optional<Asset> findById(UUID id);
    
    @Query("SELECT COUNT(a) > 0 FROM Asset a WHERE a.code = :code AND a.buildingId = :buildingId AND a.unitId IS NULL AND a.isDeleted = false")
    boolean existsByCodeAndBuildingId(@Param("code") String code, @Param("buildingId") UUID buildingId);
    
    @Query("SELECT COUNT(a) > 0 FROM Asset a WHERE a.code = :code AND a.unitId = :unitId AND a.isDeleted = false")
    boolean existsByCodeAndUnitId(@Param("code") String code, @Param("unitId") UUID unitId);
    
    Page<Asset> findByIsDeletedFalse(Pageable pageable);
    
    Page<Asset> findByBuildingIdAndIsDeletedFalse(UUID buildingId, Pageable pageable);
    
    Page<Asset> findByUnitIdAndIsDeletedFalse(UUID unitId, Pageable pageable);
    
    Page<Asset> findByBuildingIdAndAssetTypeAndIsDeletedFalse(UUID buildingId, AssetType assetType, Pageable pageable);
    
    Page<Asset> findByBuildingIdAndStatusAndIsDeletedFalse(UUID buildingId, AssetStatus status, Pageable pageable);
    
    Page<Asset> findByBuildingIdAndAssetTypeAndStatusAndIsDeletedFalse(UUID buildingId, AssetType assetType, AssetStatus status, Pageable pageable);
    
    @Query("SELECT a FROM Asset a WHERE a.isDeleted = false AND " +
           "(:buildingId IS NULL OR a.buildingId = :buildingId) AND " +
           "(:unitId IS NULL OR a.unitId = :unitId) AND " +
           "(:assetType IS NULL OR a.assetType = :assetType) AND " +
           "(:status IS NULL OR a.status = :status)")
    Page<Asset> findWithFilters(@Param("buildingId") UUID buildingId,
                                 @Param("unitId") UUID unitId,
                                 @Param("assetType") AssetType assetType,
                                 @Param("status") AssetStatus status,
                                 Pageable pageable);
    
    @Query("SELECT a FROM Asset a WHERE a.isDeleted = false AND " +
           "(LOWER(a.code) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.location) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:buildingId IS NULL OR a.buildingId = :buildingId)")
    List<Asset> searchAssets(@Param("query") String query,
                            @Param("buildingId") UUID buildingId,
                            Pageable pageable);
    
    List<Asset> findByBuildingIdAndIsDeletedFalse(UUID buildingId);
    
    List<Asset> findByUnitIdAndIsDeletedFalse(UUID unitId);


    @Query("SELECT a FROM Asset a WHERE a.assetType = :assetType AND a.isDeleted = false")
    List<Asset> findAllAssetByAssetType(@Param("assetType") AssetType assetType);
}
