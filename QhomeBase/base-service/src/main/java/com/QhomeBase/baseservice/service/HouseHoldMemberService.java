package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.dto.ContractSummary;
import com.QhomeBase.baseservice.dto.HouseholdMemberCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberUpdateDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
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
public class HouseHoldMemberService {
    
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;
    private final UnitRepository unitRepository;
    private final ContractClient contractClient;

    @Transactional
    public HouseholdMemberDto createHouseholdMember(HouseholdMemberCreateDto createDto) {
        Household household = householdRepository.findById(createDto.householdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        residentRepository.findById(createDto.residentId())
                .orElseThrow(() -> new IllegalArgumentException("Resident not found"));

        Optional<HouseholdMember> existingMember = householdMemberRepository
                .findMemberByResidentAndUnit(createDto.residentId(), household.getUnitId());

        if (existingMember.isPresent() && 
            existingMember.get().getHouseholdId().equals(createDto.householdId()) &&
            (existingMember.get().getLeftAt() == null || existingMember.get().getLeftAt().isAfter(LocalDate.now()))) {
            throw new IllegalArgumentException("Resident is already a member of this household");
        }

        if (createDto.isPrimary() != null && createDto.isPrimary()) {
            Optional<HouseholdMember> primaryMember = householdMemberRepository
                    .findPrimaryMemberByHouseholdId(createDto.householdId());
            if (primaryMember.isPresent()) {
                throw new IllegalArgumentException("Household already has a primary member");
            }
        }

        validateHouseholdCapacity(household);

        // Fetch contract - required for setting joinedAt and leftAt
        ContractSummary contract = null;
        if (household.getContractId() != null) {
            contract = fetchContractSummary(household.getContractId());
        }

        // Set joinedAt ONLY from contract.startDate (no fallback)
        LocalDate joinedAt = null;
        if (contract != null && contract.startDate() != null) {
            joinedAt = contract.startDate();
        }

        // Set leftAt ONLY from contract.endDate (no fallback)
        LocalDate leftAt = null;
        if (contract != null && contract.endDate() != null) {
            leftAt = contract.endDate();
        }

        HouseholdMember member = HouseholdMember.builder()
                .householdId(createDto.householdId())
                .residentId(createDto.residentId())
                .relation(createDto.relation())
                .proofOfRelationImageUrl(createDto.proofOfRelationImageUrl())
                .isPrimary(createDto.isPrimary() != null ? createDto.isPrimary() : false)
                .joinedAt(joinedAt)  
                .leftAt(leftAt)  
                .build();

        HouseholdMember savedMember = householdMemberRepository.save(member);
        log.info("Created household member {} for household {} (joinedAt: {} from contract/household, leftAt: {} from contract/household)", 
                savedMember.getId(), createDto.householdId(), joinedAt, leftAt);

        return toDto(savedMember);
    }

    @Transactional
    public HouseholdMemberDto updateHouseholdMember(UUID memberId, HouseholdMemberUpdateDto updateDto) {
        HouseholdMember member = householdMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));

        if (updateDto.householdId() != null) {
            householdRepository.findById(updateDto.householdId())
                    .orElseThrow(() -> new IllegalArgumentException("Household not found"));
            member.setHouseholdId(updateDto.householdId());
        }

        if (updateDto.residentId() != null) {
            residentRepository.findById(updateDto.residentId())
                    .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
            member.setResidentId(updateDto.residentId());
        }

        if (updateDto.relation() != null) {
            member.setRelation(updateDto.relation());
        }

        if (updateDto.proofOfRelationImageUrl() != null) {
            member.setProofOfRelationImageUrl(updateDto.proofOfRelationImageUrl());
        }

        if (updateDto.isPrimary() != null) {
            if (updateDto.isPrimary()) {
                Optional<HouseholdMember> existingPrimary = householdMemberRepository
                        .findPrimaryMemberByHouseholdId(member.getHouseholdId());
                if (existingPrimary.isPresent() && !existingPrimary.get().getId().equals(memberId)) {
                    throw new IllegalArgumentException("Household already has a primary member");
                }
            }
            member.setIsPrimary(updateDto.isPrimary());
        }

        if (updateDto.joinedAt() != null) {
            member.setJoinedAt(updateDto.joinedAt());
        }

        if (updateDto.leftAt() != null) {
            LocalDate joinedAt = updateDto.joinedAt() != null ? updateDto.joinedAt() : member.getJoinedAt();
            if (joinedAt != null && updateDto.leftAt().isBefore(joinedAt)) {
                throw new IllegalArgumentException("Left date cannot be before joined date");
            }
            member.setLeftAt(updateDto.leftAt());
        }

        HouseholdMember savedMember = householdMemberRepository.save(member);
        log.info("Updated household member {}", memberId);

        return toDto(savedMember);
    }

    @Transactional
    public void deleteHouseholdMember(UUID memberId) {
        HouseholdMember member = householdMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));

        member.setLeftAt(LocalDate.now());
        householdMemberRepository.save(member);
        log.info("Removed household member {} from household", memberId);
    }

    private void validateHouseholdCapacity(Household household) {
        if (household == null) {
            throw new IllegalArgumentException("Household information is required");
        }

        Unit unit = unitRepository.findById(household.getUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found for this household"));

        long activeMembers = householdMemberRepository.countActiveMembersByHouseholdId(household.getId());
        int capacity = calculateCapacity(unit);

        if (activeMembers >= capacity) {
            String unitLabel = unit.getCode() != null ? unit.getCode() : "Căn hộ";
            throw new IllegalArgumentException(String.format(
                    "%s chỉ được đăng ký tối đa %d thành viên đang sinh sống (quy tắc 1 phòng ngủ x2). Vui lòng cập nhật lại danh sách trước khi thêm mới.",
                    unitLabel,
                    capacity
            ));
        }
    }

    private int calculateCapacity(Unit unit) {
        Integer bedrooms = unit.getBedrooms();
        int effectiveBedrooms = (bedrooms == null || bedrooms <= 0) ? 1 : bedrooms;
        return Math.max(1, effectiveBedrooms) * 2;
    }

    @Transactional(readOnly = true)
    public HouseholdMemberDto getHouseholdMemberById(UUID memberId) {
        HouseholdMember member = householdMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Household member not found"));
        return toDto(member);
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberDto> getActiveMembersByHouseholdId(UUID householdId) {
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdId(householdId);
        return members.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all members by household ID (including inactive members)
     * @param householdId The household ID
     * @return List of all household member DTOs
     */
    @Transactional(readOnly = true)
    public List<HouseholdMemberDto> getAllMembersByHouseholdId(UUID householdId) {
        List<HouseholdMember> members = householdMemberRepository
                .findAllMembersByHouseholdId(householdId);
        return members.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated active members by household ID
     * @param householdId The household ID
     * @param limit Maximum number of members to return
     * @param offset Number of members to skip
     * @return Map containing members list and total count
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getActiveMembersByHouseholdIdPaged(
            UUID householdId, Integer limit, Integer offset) {
        int pageLimit = (limit != null && limit > 0) ? limit : 100; // Default 100
        int pageOffset = (offset != null && offset >= 0) ? offset : 0;
        
        // Cap limit at 1000 to prevent excessive data transfer
        pageLimit = Math.min(pageLimit, 1000);
        
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdIdWithPagination(householdId, pageLimit, pageOffset);
        
        long total = householdMemberRepository.countActiveMembersByHouseholdId(householdId);
        
        List<HouseholdMemberDto> dtos = members.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        return java.util.Map.of(
                "members", dtos,
                "total", total,
                "limit", pageLimit,
                "offset", pageOffset
        );
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberDto> getActiveMembersByResidentId(UUID residentId) {
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByResidentId(residentId);
        return members.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin household và danh sách thành viên theo residentId
     * Trả về Map chứa: unitCode, primaryResidentName, members (bao gồm cả primary resident)
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getHouseholdInfoWithMembersByResidentId(UUID residentId) {
        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByResidentId(residentId);
        
        if (members.isEmpty()) {
            return java.util.Map.of(
                "unitCode", null,
                "primaryResidentName", null,
                "primaryResidentId", null,
                "members", java.util.List.of()
            );
        }
        
        // Lấy household từ member đầu tiên (thường resident chỉ thuộc 1 household tại 1 thời điểm)
        HouseholdMember firstMember = members.get(0);
        Household household = householdRepository.findById(firstMember.getHouseholdId())
                .orElse(null);
        
        if (household == null) {
            return java.util.Map.of(
                "unitCode", null,
                "primaryResidentName", null,
                "primaryResidentId", null,
                "members", members.stream().map(this::toDto).collect(Collectors.toList())
            );
        }
        
        // Lấy unit code
        String unitCode = null;
        Unit unit = unitRepository.findById(household.getUnitId()).orElse(null);
        if (unit != null) {
            unitCode = unit.getCode();
        }
        
        // Lấy primary resident name
        String primaryResidentName = null;
        UUID primaryResidentId = household.getPrimaryResidentId();
        if (primaryResidentId != null) {
            Resident primaryResident = residentRepository.findById(primaryResidentId).orElse(null);
            if (primaryResident != null) {
                primaryResidentName = primaryResident.getFullName();
            }
        }
        
        // Lấy tất cả members trong household (bao gồm cả primary resident nếu có trong household_members)
        List<HouseholdMember> allHouseholdMembers = householdMemberRepository
                .findActiveMembersByHouseholdId(household.getId());
        
        // Nếu primary resident không có trong household_members, thêm vào danh sách
        boolean hasPrimaryInMembers = allHouseholdMembers.stream()
                .anyMatch(m -> primaryResidentId != null && m.getResidentId().equals(primaryResidentId));
        
        List<HouseholdMemberDto> memberDtos = allHouseholdMembers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        
        // Nếu primary resident không có trong members, thêm vào đầu danh sách
        if (!hasPrimaryInMembers && primaryResidentId != null && primaryResidentName != null) {
            HouseholdMemberDto primaryDto = new HouseholdMemberDto(
                    null, // id
                    household.getId(), // householdId
                    primaryResidentId, // residentId
                    primaryResidentName, // residentName
                    null, // residentEmail
                    null, // residentPhone
                    "Chủ hộ", // relation
                    null, // proofOfRelationImageUrl
                    true, // isPrimary
                    null, // joinedAt
                    null, // leftAt
                    null, // createdAt
                    null  // updatedAt
            );
            memberDtos.add(0, primaryDto);
        }
        
        return java.util.Map.of(
                "unitCode", unitCode != null ? unitCode : "",
                "primaryResidentName", primaryResidentName != null ? primaryResidentName : "",
                "primaryResidentId", primaryResidentId != null ? primaryResidentId.toString() : "",
                "members", memberDtos
        );
    }

    @Transactional(readOnly = true)
    public HouseholdMemberDto toDto(HouseholdMember member) {
        if (member == null) {
            return null;
        }

        String residentName = null;
        String residentEmail = null;
        String residentPhone = null;

        // Safely get resident info, handle null residentId
        if (member.getResidentId() != null) {
            try {
                Resident resident = residentRepository.findById(member.getResidentId()).orElse(null);
                if (resident != null) {
                    residentName = resident.getFullName();
                    residentEmail = resident.getEmail();
                    residentPhone = resident.getPhone();
                }
            } catch (Exception e) {
                log.warn("Failed to fetch resident {} for household member {}: {}", 
                        member.getResidentId(), member.getId(), e.getMessage());
                // Continue with null values
            }
        }

        return new HouseholdMemberDto(
                member.getId(),
                member.getHouseholdId(),
                member.getResidentId(),
                residentName,
                residentEmail,
                residentPhone,
                member.getRelation(),
                member.getProofOfRelationImageUrl(),
                member.getIsPrimary(),
                member.getJoinedAt(),
                member.getLeftAt(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }

    /**
     * Fetch contract summary by contract ID
     * @param contractId The contract ID
     * @return ContractSummary or null if not found or error occurs
     */
    private ContractSummary fetchContractSummary(UUID contractId) {
        if (contractId == null) {
            return null;
        }
        try {
            return contractClient.getContractById(contractId)
                    .map(contractDetail -> new ContractSummary(
                            contractDetail.id(),
                            contractDetail.unitId(),
                            contractDetail.contractNumber(),
                            contractDetail.contractType(),
                            contractDetail.startDate(),
                            contractDetail.endDate(),
                            contractDetail.status()
                    ))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to fetch contract {} for household member: {}", contractId, e.getMessage());
            return null; // Return null if contract service is unavailable or contract not found
        }
    }
}
