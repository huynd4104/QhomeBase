package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Asset;
import com.QhomeBase.baseservice.model.AssetType;
import com.QhomeBase.baseservice.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    Optional<Asset> findByAssetCode(String assetCode);

    @Query("SELECT a FROM Asset a WHERE a.unit.id = :unitId AND a.deleted = false")
    List<Asset> findByUnitId(@Param("unitId") UUID unitId);

    @Query("SELECT a FROM Asset a WHERE a.assetType = :assetType AND a.deleted = false")
    List<Asset> findByAssetType(@Param("assetType") AssetType assetType);

    @Query("SELECT a FROM Asset a WHERE a.unit.id = :unitId AND a.roomType = :roomType AND a.deleted = false")
    List<Asset> findByUnitIdAndRoomType(@Param("unitId") UUID unitId, @Param("roomType") RoomType roomType);

    @Query("SELECT a FROM Asset a WHERE a.unit.id = :unitId AND a.active = true AND a.deleted = false")
    List<Asset> findByUnitIdAndActiveTrue(@Param("unitId") UUID unitId);

    @Query("SELECT a FROM Asset a WHERE a.unit.id = :unitId AND a.assetType = :assetType AND a.active = true AND a.deleted = false")
    Optional<Asset> findByUnitAndAssetType(@Param("unitId") UUID unitId, @Param("assetType") AssetType assetType);

    @Query("""
                            SELECT a FROM Asset a
                            JOIN a.unit u
                            WHERE u.building.id = :buildingId
                              AND a.assetType = :assetType
                              AND a.active = true
                              AND a.deleted = false
                            ORDER BY u.floor, u.code, a.assetCode
                        """)
    List<Asset> findByBuildingAndAssetType(
            @Param("buildingId") UUID buildingId,
            @Param("assetType") AssetType assetType);

    @Query("""
                            SELECT a FROM Asset a
                            JOIN a.unit u
                            WHERE u.building.id = :buildingId
                              AND a.deleted = false
                            ORDER BY u.floor, u.code, a.assetCode
                        """)
    List<Asset> findByBuildingId(@Param("buildingId") UUID buildingId);

    @Query("SELECT a FROM Asset a WHERE a.active = :active AND a.deleted = false ORDER BY a.assetCode")
    List<Asset> findByActive(@Param("active") Boolean active);

    @Query("""
                            SELECT a FROM Asset a
                            JOIN a.unit u
                            WHERE u.id IN :unitIds
                              AND a.active = true
                              AND a.deleted = false
                            ORDER BY u.floor, u.code, a.assetCode
                        """)
    List<Asset> findByUnitIds(
            @Param("unitIds") List<UUID> unitIds);

    // --- Soft delete support ---
    @Query("SELECT a FROM Asset a WHERE a.deleted = true ORDER BY a.deletedAt DESC")
    List<Asset> findByDeletedTrue();

    // --- Warranty expiring support ---
    @Query("""
                            SELECT a FROM Asset a
                            WHERE a.warrantyUntil IS NOT NULL
                              AND a.warrantyUntil BETWEEN :from AND :to
                              AND a.active = true
                              AND a.deleted = false
                            ORDER BY a.warrantyUntil ASC
                        """)
    List<Asset> findByWarrantyUntilBetweenAndDeletedFalse(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // --- All non-deleted assets ---
    @Query("SELECT a FROM Asset a WHERE a.deleted = false ORDER BY a.assetCode")
    List<Asset> findAllNotDeleted();
}
