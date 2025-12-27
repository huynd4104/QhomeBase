package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.AssetInspection;
import com.QhomeBase.baseservice.model.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetInspectionRepository extends JpaRepository<AssetInspection, UUID> {

    Optional<AssetInspection> findByContractId(UUID contractId);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.unit.id = :unitId")
    List<AssetInspection> findByUnitId(@Param("unitId") UUID unitId);

    List<AssetInspection> findByStatus(InspectionStatus status);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.unit.id = :unitId AND ai.status = :status")
    List<AssetInspection> findByUnitIdAndStatus(@Param("unitId") UUID unitId, @Param("status") InspectionStatus status);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.inspectorId = :inspectorId")
    List<AssetInspection> findByInspectorId(@Param("inspectorId") UUID inspectorId);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.inspectorId = :inspectorId AND ai.status = :status")
    List<AssetInspection> findByInspectorIdAndStatus(@Param("inspectorId") UUID inspectorId, @Param("status") InspectionStatus status);

    @Query("SELECT ai FROM AssetInspection ai WHERE ai.inspectorId = :inspectorId AND ai.status IN :statuses")
    List<AssetInspection> findByInspectorIdAndStatusIn(@Param("inspectorId") UUID inspectorId, @Param("statuses") List<InspectionStatus> statuses);

    /**
     * Find completed inspections that need approval (have damage cost but no invoice yet)
     * Note: Using JPQL and filtering BigDecimal in service layer because JPQL doesn't support BigDecimal comparison
     */
    @Query("SELECT ai FROM AssetInspection ai WHERE ai.status = :status " +
           "AND ai.totalDamageCost IS NOT NULL " +
           "AND ai.invoiceId IS NULL")
    List<AssetInspection> findCompletedInspectionsPendingApproval(@Param("status") InspectionStatus status);
}

