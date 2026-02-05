package com.QhomeBase.baseservice.repository;

import com.QhomeBase.baseservice.model.Building;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    // 1. Thêm phương thức này để hỗ trợ phân trang + sắp xếp theo Code
    Page<Building> findAllByOrderByCodeAsc(Pageable pageable);

    // Các phương thức cũ của bạn giữ nguyên
    List<Building> findAllByOrderByCodeAsc();

    Building getBuildingById(UUID id);

    Optional<Building> findByCode(String code);

    Optional<Building> findByName(String name);

    @Query("SELECT b FROM Building b WHERE LOWER(b.name) = LOWER(:name)")
    List<Building> findByNameIgnoreCase(@Param("name") String name);
}