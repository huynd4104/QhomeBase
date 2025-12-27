package com.QhomeBase.datadocsservice.repository;

import com.QhomeBase.datadocsservice.model.ContractFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractFileRepository extends JpaRepository<ContractFile, UUID> {

    @Query("SELECT cf FROM ContractFile cf WHERE cf.contract.id = :contractId AND cf.isDeleted = false ORDER BY cf.displayOrder ASC, cf.uploadedAt ASC")
    List<ContractFile> findByContractId(@Param("contractId") UUID contractId);

    @Query("SELECT cf FROM ContractFile cf WHERE cf.contract.id = :contractId AND cf.isPrimary = true AND cf.isDeleted = false")
    Optional<ContractFile> findPrimaryFileByContractId(@Param("contractId") UUID contractId);

    @Query("SELECT cf FROM ContractFile cf WHERE cf.id = :id AND cf.isDeleted = false")
    Optional<ContractFile> findByIdNotDeleted(@Param("id") UUID id);
}

