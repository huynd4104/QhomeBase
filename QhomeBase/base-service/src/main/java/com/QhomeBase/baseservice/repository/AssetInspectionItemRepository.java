package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.AssetInspectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetInspectionItemRepository extends JpaRepository<AssetInspectionItem, UUID> {

    @Query("SELECT aii FROM AssetInspectionItem aii WHERE aii.inspection.id = :inspectionId")
    List<AssetInspectionItem> findByInspectionId(@Param("inspectionId") UUID inspectionId);

    @Query("SELECT aii FROM AssetInspectionItem aii JOIN FETCH aii.asset WHERE aii.inspection.id = :inspectionId")
    List<AssetInspectionItem> findByInspectionIdWithAsset(@Param("inspectionId") UUID inspectionId);
}

