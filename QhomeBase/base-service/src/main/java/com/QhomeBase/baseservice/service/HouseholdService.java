package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.dto.ContractDetailDto;
import com.QhomeBase.baseservice.dto.ContractSummary;
import com.QhomeBase.baseservice.dto.HouseholdCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdDto;
import com.QhomeBase.baseservice.dto.HouseholdUpdateDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdKind;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HouseholdService {
    
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final ContractClient contractClient;
    public Optional<UUID> getPrimaryResidentForUnit(UUID unitId) {
        return householdRepository.findCurrentHouseholdByUnitId(unitId)
                .map(Household::getPrimaryResidentId);
    }

    public boolean isPrimaryResident(UUID unitId, UUID userId) {
        Resident resident = residentRepository.findByUserId(userId).orElse(null);
        if (resident == null) {
            return false;
        }
        return getPrimaryResidentForUnit(unitId)
                .map(primaryId -> primaryId.equals(resident.getId()))
                .orElse(false);
    }

    @Transactional
    public HouseholdDto createHousehold(HouseholdCreateDto householdCreateDto) {
        if (householdCreateDto.startDate() == null) {
            throw new IllegalArgumentException("Household start date is required");
        }

        if (householdCreateDto.endDate() != null &&
                householdCreateDto.endDate().isBefore(householdCreateDto.startDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        unitRepository.findById(householdCreateDto.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        Optional<Household> existingHousehold = householdRepository.findCurrentHouseholdByUnitId(householdCreateDto.unitId());
        if (existingHousehold.isPresent()) {
            throw new IllegalArgumentException("Unit already has an active household");
        }

        if (householdCreateDto.primaryResidentId() != null) {
            residentRepository.findById(householdCreateDto.primaryResidentId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary resident not found"));
        }

        ContractSummary resolvedContract = resolveContractForHousehold(
                householdCreateDto.unitId(),
                householdCreateDto.kind(),
                householdCreateDto.contractId(),
                householdCreateDto.startDate()
        );
        UUID contractId = resolvedContract != null ? resolvedContract.id() : householdCreateDto.contractId();

        if (resolvedContract == null && householdCreateDto.kind() == HouseholdKind.OWNER) {
            throw new IllegalArgumentException("Owner household requires an active contract");
        }

        LocalDate startDate = householdCreateDto.startDate();
        LocalDate endDate = householdCreateDto.endDate();

        if (resolvedContract != null) {
            if (resolvedContract.startDate() != null && startDate.isBefore(resolvedContract.startDate())) {
                throw new IllegalArgumentException("Household start date cannot be before contract start date");
            }
            // Always use contract endDate if available (household endDate should match contract endDate)
            if (resolvedContract.endDate() != null) {
                endDate = resolvedContract.endDate();
            }
        }

        Household household = Household.builder()
                .unitId(householdCreateDto.unitId())
                .kind(householdCreateDto.kind())
                .primaryResidentId(householdCreateDto.primaryResidentId())
                .startDate(startDate)
                .endDate(endDate)
                .contractId(contractId)
                .build();

        Household savedHousehold = householdRepository.save(household);
        log.info("Created household {} for unit {}", savedHousehold.getId(), householdCreateDto.unitId());

        return toDto(savedHousehold);
    }

    @Transactional
    public HouseholdDto updateHousehold(UUID householdId, HouseholdUpdateDto updateDto) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        if (updateDto.startDate() != null) {
            if (updateDto.startDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Start date cannot be in the past");
            }
            household.setStartDate(updateDto.startDate());
        }

        if (updateDto.endDate() != null) {
            LocalDate startDate = updateDto.startDate() != null ? updateDto.startDate() : household.getStartDate();
            if (updateDto.endDate().isBefore(startDate)) {
                throw new IllegalArgumentException("End date cannot be before start date");
            }
            household.setEndDate(updateDto.endDate());
        }

        if (updateDto.unitId() != null) {
            unitRepository.findById(updateDto.unitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found"));
            household.setUnitId(updateDto.unitId());
        }

        if (updateDto.kind() != null) {
            household.setKind(updateDto.kind());
        }

        if (updateDto.contractId() != null) {
            LocalDate effectiveStartDate = updateDto.startDate() != null
                    ? updateDto.startDate()
                    : household.getStartDate();
            ContractSummary resolvedContract = resolveContractById(
                    updateDto.contractId(),
                    household.getUnitId(),
                    effectiveStartDate
            );
            if (resolvedContract == null) {
                throw new IllegalArgumentException("Contract not found or invalid for this unit");
            }
            household.setContractId(resolvedContract.id());
        }

        if (updateDto.primaryResidentId() != null) {
            residentRepository.findById(updateDto.primaryResidentId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary resident not found"));
            household.setPrimaryResidentId(updateDto.primaryResidentId());
        }

        Household savedHousehold = householdRepository.save(household);
        log.info("Updated household {}", householdId);

        return toDto(savedHousehold);
    }

    @Transactional
    public void deleteHousehold(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        // Set endDate to 2 days ago to ensure household is no longer "current"
        // This accounts for potential timezone differences between Java LocalDate.now() and PostgreSQL CURRENT_DATE
        // findCurrentHouseholdByUnitId() checks: endDate IS NULL OR endDate >= CURRENT_DATE
        // By setting endDate = 2 days ago, we ensure endDate < CURRENT_DATE even with timezone differences
        LocalDate deactivationDate = LocalDate.now().minusDays(2);
        household.setEndDate(deactivationDate);
        householdRepository.save(household);
        log.info("Deactivated household {} (endDate set to {} - 2 days ago to account for timezone differences)", 
                householdId, deactivationDate);
        
        // Determine leftAt date for members: use contract endDate if available, otherwise use household endDate
        LocalDate membersLeftAt = determineMembersLeftAt(household);
        
       
        // By setting leftAt = contract.endDate (or household.endDate), we ensure leftAt < CURRENT_DATE
        deactivateHouseholdMembers(householdId, membersLeftAt);
    }
    
    private LocalDate determineMembersLeftAt(Household household) {
        // Try to get contract endDate first
        if (household.getContractId() != null) {
            try {
                ContractSummary contract = fetchContractSummary(household.getContractId());
                if (contract != null && contract.endDate() != null) {
                    log.info("Using contract endDate {} as members leftAt for household {}", 
                            contract.endDate(), household.getId());
                    return contract.endDate();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch contract {} for household {}: {}", 
                        household.getContractId(), household.getId(), e.getMessage());
            }
        }
        
        // Fallback to household endDate if available
        if (household.getEndDate() != null) {
            log.info("Using household endDate {} as members leftAt for household {}", 
                    household.getEndDate(), household.getId());
            return household.getEndDate();
        }
        
        // Final fallback: use 2 days ago (same as household deactivationDate)
        LocalDate fallbackDate = LocalDate.now().minusDays(2);
        log.info("Using fallback date {} (2 days ago) as members leftAt for household {}", 
                fallbackDate, household.getId());
        return fallbackDate;
    }
    
    private void deactivateHouseholdMembers(UUID householdId, LocalDate leftAtDate) {
        // Find all active household members (where leftAt IS NULL or leftAt >= CURRENT_DATE)
        // We use findActiveMembersByHouseholdId which checks: leftAt IS NULL OR leftAt >= CURRENT_DATE
        // After setting leftAt = leftAtDate (contract.endDate or household.endDate), the query won't find them anymore
        List<HouseholdMember> activeMembers = 
                householdMemberRepository.findActiveMembersByHouseholdId(householdId);
        
        for (com.QhomeBase.baseservice.model.HouseholdMember member : activeMembers) {
            member.setLeftAt(leftAtDate);
            householdMemberRepository.save(member);
            log.debug("Deactivated household member {} (leftAt set to {})", member.getId(), leftAtDate);
        }
        
        if (!activeMembers.isEmpty()) {
            log.info("Deactivated {} household member(s) for household {} (leftAt set to {} - contract endDate or household endDate)", 
                    activeMembers.size(), householdId, leftAtDate);
        }
    }

    @Transactional(readOnly = true)
    public HouseholdDto getHouseholdById(UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));
        return toDto(household);
    }

    @Transactional(readOnly = true)
    public HouseholdDto getCurrentHouseholdByUnitId(UUID unitId) {
        // OPTIMIZED: Uses composite index idx_households_unit_end_date for fast lookup
        Household household = householdRepository.findCurrentHouseholdByUnitId(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit has no active household"));
        
        // OPTIMIZED: Use findByIdWithBuilding to avoid additional query for building
        String unitCode = null;
        String primaryResidentName = null;
        
        // Use optimized query that already JOIN FETCHes building
        try {
            Unit unit = unitRepository.findByIdWithBuilding(household.getUnitId());
            if (unit != null) {
                unitCode = unit.getCode();
            }
        } catch (Exception e) {
            // Unit not found or error loading - continue without unitCode
            log.warn("[HouseholdService] Failed to load unit {}: {}", household.getUnitId(), e.getMessage());
        }

        // Load resident only if needed
        if (household.getPrimaryResidentId() != null) {
            Resident primaryResident = residentRepository.findById(household.getPrimaryResidentId()).orElse(null);
            if (primaryResident != null) {
                primaryResidentName = primaryResident.getFullName();
            }
        }
        
        UUID contractId = household.getContractId();
        
        // Transaction ends here - connection is released
        
        // Fetch contract summary AFTER transaction (external HTTP call)
        ContractSummary contract = null;
        if (contractId != null) {
            try {
                contract = fetchContractSummary(contractId);
            } catch (Exception e) {
                // Continue without contract summary - not critical
            }
        }
        
        return new HouseholdDto(
                household.getId(),
                household.getUnitId(),
                unitCode,
                household.getKind(),
                household.getPrimaryResidentId(),
                primaryResidentName,
                household.getStartDate(),
                household.getEndDate(),
                contract != null ? contract.id() : null,
                contract != null ? contract.contractNumber() : null,
                contract != null ? contract.startDate() : null,
                contract != null ? contract.endDate() : null,
                contract != null ? contract.status() : null,
                household.getCreatedAt(),
                household.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<HouseholdDto> getAllHouseholdsByUnitId(UUID unitId) {
        List<Household> households = householdRepository.findAll()
                .stream()
                .filter(h -> h.getUnitId().equals(unitId))
                .collect(Collectors.toList());

        return households.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UUID getPayerForUnit(UUID unitId) {
        return getPrimaryResidentForUnit(unitId).orElse(null);
    }

    public HouseholdDto toDto(Household household) {
        if (household == null) {
            return null;
        }

        String unitCode = null;
        Unit unit = unitRepository.findById(household.getUnitId()).orElse(null);
        if (unit != null) {
            unitCode = unit.getCode();
        }

        String primaryResidentName = null;
        if (household.getPrimaryResidentId() != null) {
            Resident primaryResident = residentRepository.findById(household.getPrimaryResidentId()).orElse(null);
            if (primaryResident != null) {
                primaryResidentName = primaryResident.getFullName();
            }
        }

        // Fetch contract summary AFTER transaction (external HTTP call)
        // Note: This method is called from @Transactional methods, so we fetch contract outside transaction
        ContractSummary contract = null;
        if (household.getContractId() != null) {
            try {
                contract = fetchContractSummary(household.getContractId());
            } catch (Exception e) {
                log.warn("⚠️ [HouseholdService] Failed to fetch contract summary for contract {}: {}", 
                        household.getContractId(), e.getMessage());
                // Continue without contract summary
            }
        }

        return new HouseholdDto(
                household.getId(),
                household.getUnitId(),
                unitCode,
                household.getKind(),
                household.getPrimaryResidentId(),
                primaryResidentName,
                household.getStartDate(),
                household.getEndDate(),
                contract != null ? contract.id() : null,
                contract != null ? contract.contractNumber() : null,
                contract != null ? contract.startDate() : null,
                contract != null ? contract.endDate() : null,
                contract != null ? contract.status() : null,
                household.getCreatedAt(),
                household.getUpdatedAt()
        );
    }

    private ContractSummary resolveContractForHousehold(
            UUID unitId,
            HouseholdKind kind,
            UUID contractId,
            LocalDate householdStartDate
    ) {
        if (kind != HouseholdKind.OWNER) {
            return null;
        }

        if (contractId != null) {
            return resolveContractById(contractId, unitId, householdStartDate);
        }

        return contractClient.findFirstActiveContract(unitId)
                .orElseThrow(() -> new IllegalArgumentException("No active contract found for unit " + unitId));
    }

    private ContractSummary resolveContractById(UUID contractId, UUID expectedUnitId, LocalDate householdStartDate) {
        ContractDetailDto contract = contractClient.getContractById(contractId)
                .filter(dto -> dto.unitId() != null && dto.unitId().equals(expectedUnitId))
                .orElseThrow(() -> new IllegalArgumentException("Contract " + contractId + " not found for unit " + expectedUnitId));

        LocalDate referenceDate = householdStartDate != null ? householdStartDate : LocalDate.now();
        boolean isActive = isContractActiveOn(contract, referenceDate);
        boolean isPending = contract.status() != null && contract.status().equalsIgnoreCase("PENDING");
        boolean dateValid = contract.startDate() == null || !referenceDate.isBefore(contract.startDate());

        if (!isActive && !(isPending && dateValid)) {
            throw new IllegalArgumentException("Contract " + contractId + " is not active for unit " + expectedUnitId);
        }

        return summarizeContract(contract);
    }

    private ContractSummary fetchContractSummary(UUID contractId) {
        if (contractId == null) {
            return null;
        }
        try {
            return contractClient.getContractById(contractId)
                    .map(this::summarizeContract)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to fetch contract {}: {}", contractId, e.getMessage());
            return null; // Return null if contract service is unavailable or contract not found
        }
    }

    private ContractSummary summarizeContract(ContractDetailDto contract) {
        return new ContractSummary(
                contract.id(),
                contract.unitId(),
                contract.contractNumber(),
                contract.contractType(),
                contract.startDate(),
                contract.endDate(),
                contract.status()
        );
    }

    private boolean isContractActiveOn(ContractDetailDto contract, LocalDate date) {
        if (date == null) {
            return true;
        }
        boolean afterStart = contract.startDate() == null || !date.isBefore(contract.startDate());
        boolean beforeEnd = contract.endDate() == null || !date.isAfter(contract.endDate());
        boolean statusActive = contract.status() == null || contract.status().equalsIgnoreCase("ACTIVE");
        return afterStart && beforeEnd && statusActive;
    }
}
