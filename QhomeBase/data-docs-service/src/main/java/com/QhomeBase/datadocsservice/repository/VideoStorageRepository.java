package com.QhomeBase.datadocsservice.repository;

import com.QhomeBase.datadocsservice.model.VideoStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoStorageRepository extends JpaRepository<VideoStorage, UUID> {
    
    List<VideoStorage> findByCategoryAndOwnerIdAndIsDeletedFalse(String category, UUID ownerId);
    
    List<VideoStorage> findByCategoryAndIsDeletedFalse(String category);
    
    Optional<VideoStorage> findByIdAndIsDeletedFalse(UUID id);
    
    @Query("SELECT v FROM VideoStorage v WHERE v.category = :category AND v.ownerId = :ownerId AND v.isDeleted = false ORDER BY v.uploadedAt DESC")
    List<VideoStorage> findActiveVideosByCategoryAndOwner(@Param("category") String category, @Param("ownerId") UUID ownerId);
}
