package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Asset;
import com.QhomeBase.baseservice.model.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    
    Optional<Asset> findByAssetCode(String assetCode);
    
    List<Asset> findByUnitId(UUID unitId);
    
    List<Asset> findByAssetType(AssetType assetType);
    
    @Query("SELECT a FROM Asset a WHERE a.unit.id = :unitId AND a.assetType = :assetType AND a.active = true")
    Optional<Asset> findByUnitAndAssetType(@Param("unitId") UUID unitId, @Param("assetType") AssetType assetType);
    
    @Query("""
        SELECT a FROM Asset a
        JOIN a.unit u
        WHERE u.building.id = :buildingId
          AND a.assetType = :assetType
          AND a.active = true
        ORDER BY u.floor, u.code, a.assetCode
    """)
    List<Asset> findByBuildingAndAssetType(
        @Param("buildingId") UUID buildingId,
        @Param("assetType") AssetType assetType
    );
    
    @Query("""
        SELECT a FROM Asset a
        JOIN a.unit u
        WHERE u.building.id = :buildingId
        ORDER BY u.floor, u.code, a.assetCode
    """)
    List<Asset> findByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("SELECT a FROM Asset a WHERE a.active = :active ORDER BY a.assetCode")
    List<Asset> findByActive(@Param("active") Boolean active);
    
    @Query("""
        SELECT a FROM Asset a
        JOIN a.unit u
        WHERE u.id IN :unitIds
          AND a.active = true
        ORDER BY u.floor, u.code, a.assetCode
    """)
    List<Asset> findByUnitIds(
        @Param("unitIds") List<UUID> unitIds
    );
}


