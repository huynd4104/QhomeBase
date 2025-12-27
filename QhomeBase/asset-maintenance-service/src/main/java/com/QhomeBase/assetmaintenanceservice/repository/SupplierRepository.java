package com.QhomeBase.assetmaintenanceservice.repository;

import com.QhomeBase.assetmaintenanceservice.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    
    Optional<Supplier> findById(UUID id);
    
    Page<Supplier> findAll(Pageable pageable);
    
    Page<Supplier> findByIsActiveTrue(Pageable pageable);
    
    Page<Supplier> findByIsActiveFalse(Pageable pageable);
    
    @Query("SELECT s FROM Supplier s WHERE " +
           "(:isActive IS NULL OR s.isActive = :isActive) AND " +
           "(:type IS NULL OR LOWER(s.type) = LOWER(:type))")
    Page<Supplier> findWithFilters(@Param("isActive") Boolean isActive,
                                    @Param("type") String type,
                                    Pageable pageable);
    
    @Query("SELECT s FROM Supplier s WHERE s.isActive = true AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.phone) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Supplier> searchSuppliers(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE LOWER(s.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);
    
    @Query("SELECT COUNT(s) > 0 FROM Supplier s WHERE LOWER(s.name) = LOWER(:name) AND s.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") UUID id);
    
    List<Supplier> findByIsActiveTrue();
    
    List<Supplier> findByTypeAndIsActiveTrue(String type);
}

