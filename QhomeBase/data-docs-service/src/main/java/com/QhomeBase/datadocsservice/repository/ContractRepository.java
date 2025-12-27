package com.QhomeBase.datadocsservice.repository;

import com.QhomeBase.datadocsservice.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.id = :id")
    Optional<Contract> findByIdWithFiles(@Param("id") UUID id);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.unitId = :unitId")
    List<Contract> findByUnitId(@Param("unitId") UUID unitId);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.unitId = :unitId AND c.status = :status")
    List<Contract> findByUnitIdAndStatus(@Param("unitId") UUID unitId, @Param("status") String status);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files WHERE c.contractNumber = :contractNumber")
    Optional<Contract> findByContractNumber(@Param("contractNumber") String contractNumber);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files " +
           "WHERE c.status = 'ACTIVE' " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate)")
    List<Contract> findActiveContracts(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT c FROM Contract c LEFT JOIN FETCH c.files " +
           "WHERE c.unitId = :unitId " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.endDate IS NULL OR c.endDate >= :currentDate)")
    List<Contract> findActiveContractsByUnit(@Param("unitId") UUID unitId, @Param("currentDate") LocalDate currentDate);

    @Query("SELECT c FROM Contract c WHERE c.status = 'INACTIVE' AND c.startDate = :targetDate")
    List<Contract> findInactiveContractsByStartDate(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.contractType = 'RENTAL' " +
           "AND c.endDate IS NOT NULL " +
           "AND c.endDate >= :startDate " +
           "AND c.endDate <= :endDate " +
           "AND (c.renewalStatus = 'PENDING' OR c.renewalStatus = 'REMINDED')")
    List<Contract> findContractsNeedingRenewalReminder(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find contracts that need renewal reminders based on days until end date
     * This includes contracts with endDate in the next 0-32 days
     * (0 days để bao gồm contracts sắp hết hạn cho reminder 3)
     */
    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.contractType = 'RENTAL' " +
           "AND c.endDate IS NOT NULL " +
           "AND c.endDate >= :today " +
           "AND c.endDate <= :maxDate " +
           "AND (c.renewalStatus = 'PENDING' OR c.renewalStatus = 'REMINDED')")
    List<Contract> findContractsNeedingRenewalReminderByDateRange(
        @Param("today") LocalDate today,
        @Param("maxDate") LocalDate maxDate
    );

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.contractType = 'RENTAL' " +
           "AND c.renewalStatus = 'REMINDED' " +
           "AND c.renewalReminderSentAt IS NOT NULL " +
           "AND c.renewalReminderSentAt <= :deadlineDate")
    List<Contract> findContractsWithRenewalDeclined(
        @Param("deadlineDate") java.time.OffsetDateTime deadlineDate
    );

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.endDate IS NOT NULL " +
           "AND c.endDate < :today")
    List<Contract> findContractsNeedingExpired(@Param("today") java.time.LocalDate today);

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.contractType = 'RENTAL' " +
           "AND c.endDate IS NOT NULL " +
           "AND c.renewalStatus = 'REMINDED' " +
           "AND c.renewalReminderSentAt IS NOT NULL " +
           "AND c.renewalReminderSentAt <= :sevenDaysAgo")
    List<Contract> findContractsNeedingSecondReminder(
        @Param("sevenDaysAgo") java.time.OffsetDateTime sevenDaysAgo
    );

    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.contractType = 'RENTAL' " +
           "AND c.endDate IS NOT NULL " +
           "AND c.renewalStatus = 'REMINDED' " +
           "AND c.renewalReminderSentAt IS NOT NULL " +
           "AND c.renewalReminderSentAt <= :twentyDaysAgo")
    List<Contract> findContractsNeedingThirdReminder(
        @Param("twentyDaysAgo") java.time.OffsetDateTime twentyDaysAgo
    );

}

