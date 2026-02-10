package com.QhomeBase.iamservice.repository;

import com.QhomeBase.iamservice.model.StaffProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {
    Optional<StaffProfile> findByUserId(UUID userId);

    Optional<StaffProfile> findByPhone(String phone);

    Optional<StaffProfile> findByNationalId(String nationalId);
}
