package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.VehicleRegistrationRequest;
import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VehicleRegistrationRepository extends JpaRepository<VehicleRegistrationRequest, UUID> {
    
    List<VehicleRegistrationRequest> findAllByStatus(VehicleRegistrationStatus status);
    
    List<VehicleRegistrationRequest> findAllByRequestedBy(UUID requestedBy);
    
    List<VehicleRegistrationRequest> findAllByApprovedBy(UUID approvedBy);
    
    List<VehicleRegistrationRequest> findAllByVehicleId(UUID vehicleId);
    
    @Query("SELECT v FROM VehicleRegistrationRequest v WHERE " +
           "(LOWER(v.reason) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.note) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<VehicleRegistrationRequest> searchByTerm(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT v FROM VehicleRegistrationRequest v JOIN FETCH v.vehicle")
    List<VehicleRegistrationRequest> findAllWithVehicle();
    
    @Query("SELECT v FROM VehicleRegistrationRequest v JOIN FETCH v.vehicle WHERE v.id = :id")
    VehicleRegistrationRequest findByIdWithVehicle(@Param("id") UUID id);
    
    boolean existsByVehicleId(UUID vehicleId);
    
    boolean existsByVehicleIdAndIdNot(UUID vehicleId, UUID id);
    
    /**
     * Find vehicle registration requests by building and status
     * Useful for filtering pending requests by building
     */
    @Query("""
        SELECT vrr FROM VehicleRegistrationRequest vrr
        JOIN FETCH vrr.vehicle v
        LEFT JOIN FETCH v.unit u
        LEFT JOIN FETCH u.building b
        WHERE b.id = :buildingId
          AND vrr.status = :status
        ORDER BY vrr.requestedAt DESC
        """)
    List<VehicleRegistrationRequest> findByBuildingAndStatus(
        @Param("buildingId") UUID buildingId,
        @Param("status") VehicleRegistrationStatus status
    );
    
    /**
     * Find all pending requests by building
     */
    @Query("""
        SELECT vrr FROM VehicleRegistrationRequest vrr
        JOIN FETCH vrr.vehicle v
        LEFT JOIN FETCH v.unit u
        LEFT JOIN FETCH u.building b
        WHERE b.id = :buildingId
          AND vrr.status = 'PENDING'
        ORDER BY vrr.requestedAt DESC
        """)
    List<VehicleRegistrationRequest> findPendingByBuilding(@Param("buildingId") UUID buildingId);
}

