package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.dto.ContractDetailDto;
import com.QhomeBase.baseservice.dto.ContractSummary;
import com.QhomeBase.baseservice.dto.CreateResidentAccountDto;
import com.QhomeBase.baseservice.dto.PrimaryResidentProvisionRequest;
import com.QhomeBase.baseservice.dto.PrimaryResidentProvisionResponse;
import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.HouseholdKind;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountProvideService {

    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UnitRepository unitRepository;
    private final ResidentAccountService residentAccountService;
    private final ContractClient contractClient;

    @Transactional
    public PrimaryResidentProvisionResponse provisionPrimaryResident(
            UUID unitId,
            PrimaryResidentProvisionRequest request,
            String token
    ) {
        HouseholdProvisionContext context = ensureActiveHousehold(unitId);
        Household household = context.household();

        if (household.getPrimaryResidentId() != null) {
            throw new IllegalStateException("Household already has a primary resident");
        }

       
        Resident resident;
        String nationalId = request.resident().nationalId();
        if (nationalId != null && !nationalId.isBlank()) {
            resident = residentRepository.findByNationalId(nationalId).orElse(null);
            if (resident != null) {
                log.info("Reusing existing resident {} (nationalId: {}) for unit {}", resident.getId(), nationalId, unitId);
              
                String phone = request.resident().phone();
                if (phone != null && !phone.isBlank() && !phone.equals(resident.getPhone())) {
                    if (residentRepository.existsByPhone(phone)) {
                        throw new IllegalArgumentException("There is already a resident with that phone");
                    }
                    resident.setPhone(phone);
                }
                
                String email = request.resident().email();
                if (email != null && !email.isBlank() && !email.equals(resident.getEmail())) {
                    if (residentRepository.existsByEmail(email)) {
                        throw new IllegalArgumentException("There is already a resident with that email");
                    }
                    resident.setEmail(email);
                }
                if (request.resident().fullName() != null && !request.resident().fullName().isBlank()) {
                    resident.setFullName(request.resident().fullName());
                }
                if (request.resident().dob() != null) {
                    resident.setDob(request.resident().dob());
                }
                if (request.resident().status() != null) {
                    resident.setStatus(request.resident().status());
                }
                resident = residentRepository.save(resident);
            } else {
                validateUniqueContact(request);
                Resident.ResidentBuilder builder = Resident.builder()
                        .fullName(request.resident().fullName())
                        .phone(request.resident().phone())
                        .email(request.resident().email())
                        .nationalId(request.resident().nationalId())
                        .dob(request.resident().dob());

                if (request.resident().status() != null) {
                    builder.status(request.resident().status());
                }

                resident = builder.build();
                resident = residentRepository.save(resident);
            }
        } else {
           
            validateUniqueContact(request);
            Resident.ResidentBuilder builder = Resident.builder()
                    .fullName(request.resident().fullName())
                    .phone(request.resident().phone())
                    .email(request.resident().email())
                    .nationalId(request.resident().nationalId())
                    .dob(request.resident().dob());

            if (request.resident().status() != null) {
                builder.status(request.resident().status());
            }

            resident = builder.build();
            resident = residentRepository.save(resident);
        }

        try {

            household.setPrimaryResidentId(resident.getId());
            household.setUpdatedAt(OffsetDateTime.now());
            householdRepository.save(household);

           
            ContractSummary contract = null;
            if (household.getContractId() != null) {
                contract = fetchContractSummary(household.getContractId());
            }

          
            LocalDate joinedAt = null;
            if (contract != null && contract.startDate() != null) {
                joinedAt = contract.startDate();
            }

            
            LocalDate leftAt = null;
            if (contract != null && contract.endDate() != null) {
                leftAt = contract.endDate();
            }

            HouseholdMember householdMember = HouseholdMember.builder()
                    .householdId(household.getId())
                    .residentId(resident.getId())
                    .relation(resolveRelation(request))
                    .isPrimary(true)
                    .joinedAt(joinedAt)  // From contract.startDate only
                    .leftAt(leftAt)  // From contract.endDate only
                    .build();

            householdMember = householdMemberRepository.save(householdMember);

            CreateResidentAccountDto accountRequest = request.account();
            if (accountRequest == null) {
                accountRequest = new CreateResidentAccountDto(null, null, true);
            }

            ResidentAccountDto account = residentAccountService.createAccountForResidentAsAdmin(
                    resident.getId(),
                    accountRequest,
                    token
            );

            log.info("Provisioned primary resident {} for household {} (unit {})", resident.getId(), household.getId(), unitId);

            return new PrimaryResidentProvisionResponse(
                    resident.getId(),
                    householdMember.getId(),
                    account
            );
        } catch (RuntimeException ex) {
            rollbackHouseholdIfNeeded(context);
            throw ex;
        }
    }

    private HouseholdProvisionContext ensureActiveHousehold(UUID unitId) {
        return householdRepository.findCurrentHouseholdByUnitId(unitId)
                .map(existing -> {
                    ensureHouseholdHasContract(existing);
                    return new HouseholdProvisionContext(existing, false);
                })
                .orElseGet(() -> createHouseholdForProvision(unitId));
    }

    private void rollbackHouseholdIfNeeded(HouseholdProvisionContext context) {
        if (context.createdForProvision()) {
            householdRepository.delete(context.household());
            log.info("Deleted temporary household {} after provisioning failure", context.household().getId());
        }
    }

    private HouseholdProvisionContext createHouseholdForProvision(UUID unitId) {
        unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        ContractSummary contract = contractClient.findFirstActiveContract(unitId)
                .orElseThrow(() -> new IllegalStateException("Unit has no active contract. Cannot provision primary resident."));

        LocalDate startDate = contract.startDate() != null ? contract.startDate() : LocalDate.now();

        Household newHousehold = Household.builder()
                .unitId(unitId)
                .kind(HouseholdKind.OWNER)
                .startDate(startDate)
                .endDate(contract.endDate())
                .contractId(contract.id())
                .build();

        Household saved = householdRepository.save(newHousehold);
        log.info("Created household {} for unit {} (contract {}) during primary resident provisioning", saved.getId(), unitId, contract.id());
        return new HouseholdProvisionContext(saved, true);
    }

    private void ensureHouseholdHasContract(Household household) {
        if (household.getContractId() != null) {
            return;
        }
        contractClient.findFirstActiveContract(household.getUnitId())
                .ifPresent(contract -> {
                    household.setContractId(contract.id());
                    if (household.getStartDate() == null) {
                        household.setStartDate(contract.startDate() != null ? contract.startDate() : LocalDate.now());
                    }
                    if (household.getEndDate() == null && contract.endDate() != null) {
                        household.setEndDate(contract.endDate());
                    }
                    householdRepository.save(household);
                    log.info("Linked household {} with active contract {}", household.getId(), contract.id());
                });
    }

    private record HouseholdProvisionContext(Household household, boolean createdForProvision) {}

    private void validateUniqueContact(PrimaryResidentProvisionRequest request) {
        // Phone and email should remain unique as they are used for account creation
        String phone = request.resident().phone();
        if (phone != null && !phone.isBlank() && residentRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("There is already a resident with that phone");
        }

        String email = request.resident().email();
        if (email != null && !email.isBlank()) {
            // Ensure email contains exactly one @
            long atCount = email.chars().filter(ch -> ch == '@').count();
            if (atCount != 1) {
                throw new IllegalArgumentException("Email phải có đúng 1 ký tự @");
            }
            // Validate email ending with .com
            String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.com$";
            if (!email.matches(emailPattern)) {
                throw new IllegalArgumentException("Email phải có đuôi .com. Ví dụ: user@example.com");
            }
            if (residentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("There is already a resident with that email");
            }
        }

        // National ID validation removed - allow reusing existing resident
        // A person can be primary resident of multiple units (e.g., owns multiple apartments)
    }

    private String resolveRelation(PrimaryResidentProvisionRequest request) {
        String relation = request.relation();
        if (relation == null || relation.isBlank()) {
            return "Chủ hộ";
        }
        return relation;
    }

 
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
            log.warn("Failed to fetch contract {} for primary resident provisioning: {}", contractId, e.getMessage());
            return null; // Return null if contract service is unavailable or contract not found
        }
    }
}
