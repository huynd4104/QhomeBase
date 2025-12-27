package com.QhomeBase.servicescardservice.repository;

import com.QhomeBase.servicescardservice.model.RegisterServiceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RegisterServiceImageRepository extends JpaRepository<RegisterServiceImage, UUID> {
    void deleteByRegisterServiceRequestId(UUID registerServiceRequestId);
}


