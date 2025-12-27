package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.HouseholdMemberCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDecisionDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestDto;
import com.QhomeBase.baseservice.dto.HouseholdMemberRequestResendDto;
import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMemberRequest;
import com.QhomeBase.baseservice.model.HouseholdMemberRequest.RequestStatus;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdMemberRequestRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseholdMemberRequestService {

    private final HouseholdMemberRequestRepository requestRepository;
    private final HouseholdRepository householdRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseHoldMemberService houseHoldMemberService;
    private final UnitRepository unitRepository;
    private final IamClientService iamClientService;
    private final NotificationClient notificationClient;

    @Transactional
    public HouseholdMemberRequestDto createRequest(HouseholdMemberRequestCreateDto createDto, UserPrincipal principal) {
        Household household = householdRepository.findById(createDto.householdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        Resident requesterResident = residentRepository.findByUserId(principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Requester is not linked to any resident"));

        if (!isPrimaryResident(household, requesterResident.getId())) {
            throw new IllegalArgumentException("Only the primary resident can submit membership requests");
        }

        validateEmailUniqueness(createDto, requesterResident, principal);
        validateNationalIdUniqueness(createDto);
        validatePhoneUniqueness(createDto);

        UUID resolvedResidentId = resolveExistingResident(createDto);

        if (resolvedResidentId != null) {
            requestRepository.findFirstByHouseholdIdAndResidentIdAndStatusIn(
                    createDto.householdId(),
                    resolvedResidentId,
                    List.of(RequestStatus.PENDING)
            ).ifPresent(existing -> {
                throw new IllegalArgumentException("There is already a pending request for this resident");
            });

            householdMemberRepository.findActiveMemberByResidentAndHousehold(resolvedResidentId, createDto.householdId())
                    .ifPresent(member -> {
                        throw new IllegalArgumentException("Resident is already a member of this household");
                    });
        } else if (createDto.residentPhone() != null && !createDto.residentPhone().isBlank()) {
            requestRepository.findFirstByHouseholdIdAndResidentFullNameIgnoreCaseAndResidentPhoneAndStatusIn(
                    createDto.householdId(),
                    createDto.residentFullName(),
                    createDto.residentPhone(),
                    List.of(RequestStatus.PENDING)
            ).ifPresent(existing -> {
                throw new IllegalArgumentException("There is already a pending request for this resident");
            });
        }

        ensureHouseholdHasCapacity(household);

        HouseholdMemberRequest request = HouseholdMemberRequest.builder()
                .householdId(createDto.householdId())
                .residentId(resolvedResidentId)
                .requestedBy(principal.uid())
                .residentFullName(createDto.residentFullName())
                .residentPhone(createDto.residentPhone())
                .residentEmail(createDto.residentEmail())
                .residentNationalId(createDto.residentNationalId())
                .residentDob(createDto.residentDob())
                .relation(createDto.relation())
                .proofOfRelationImageUrl(createDto.proofOfRelationImageUrl())
                .note(createDto.note())
                .status(RequestStatus.PENDING)
                .build();

        HouseholdMemberRequest saved = requestRepository.save(request);
        log.info("Household member request {} created by {}", saved.getId(), principal.uid());

        return toDto(saved);
    }

    @Transactional
    public HouseholdMemberRequestDto decideRequest(UUID requestId, HouseholdMemberRequestDecisionDto decisionDto, UUID adminUserId) {
        HouseholdMemberRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Household member request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Request has already been processed");
        }

        if (Boolean.TRUE.equals(decisionDto.approve())) {
            approveRequest(request, adminUserId);
        } else {
            rejectRequest(request, adminUserId, decisionDto.rejectionReason());
        }

        HouseholdMemberRequest saved = requestRepository.save(request);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberRequestDto> getRequestsForUser(UUID requesterUserId) {
        return requestRepository.findByRequestedByOrderByCreatedAtDesc(requesterUserId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HouseholdMemberRequestDto> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public HouseholdMemberRequestDto cancelRequest(UUID requestId, UUID requesterUserId) {
        HouseholdMemberRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Household member request not found"));

        if (!requesterUserId.equals(request.getRequestedBy())) {
            throw new IllegalArgumentException("Bạn không có quyền hủy yêu cầu này");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể hủy các yêu cầu đang chờ duyệt");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setRejectedBy(requesterUserId);
        request.setRejectedAt(OffsetDateTime.now());
        request.setRejectionReason("Cư dân đã hủy yêu cầu");
        request.setUpdatedAt(OffsetDateTime.now());

        HouseholdMemberRequest saved = requestRepository.save(request);
        log.info("Household member request {} cancelled by {}", requestId, requesterUserId);
        return toDto(saved);
    }

    /**
     * Resend a rejected household member request.
     * Creates a new request with PENDING status based on the rejected request.
     * The original request remains REJECTED for audit purposes.
     * Only the Owner who created the original request can resend it.
     */
    @Transactional
    public HouseholdMemberRequestDto resendRequest(UUID requestId, HouseholdMemberRequestResendDto resendDto, UserPrincipal principal) {
        HouseholdMemberRequest originalRequest = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Household member request not found"));

        // Check permission: only the Owner who created the request can resend it
        if (!principal.uid().equals(originalRequest.getRequestedBy())) {
            throw new IllegalArgumentException("Bạn không có quyền gửi lại yêu cầu này. Chỉ chủ căn hộ tạo yêu cầu ban đầu mới có quyền gửi lại.");
        }

        // Check that original request is REJECTED
        if (originalRequest.getStatus() != RequestStatus.REJECTED) {
            if (originalRequest.getStatus() == RequestStatus.APPROVED) {
                throw new IllegalArgumentException("Không thể gửi lại yêu cầu này vì yêu cầu đã được duyệt");
            }
            throw new IllegalArgumentException("Chỉ có thể gửi lại các yêu cầu đã bị từ chối. Trạng thái hiện tại: " + originalRequest.getStatus());
        }

        Household household = householdRepository.findById(originalRequest.getHouseholdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        Resident requesterResident = residentRepository.findByUserId(principal.uid())
                .orElseThrow(() -> new IllegalArgumentException("Requester is not linked to any resident"));

        if (!isPrimaryResident(household, requesterResident.getId())) {
            throw new IllegalArgumentException("Chỉ chủ căn hộ mới được gửi lại yêu cầu đăng ký thành viên");
        }

        // Use updated values from resendDto if provided, otherwise use original request values
        String residentFullName = resendDto.residentFullName() != null && !resendDto.residentFullName().isBlank()
                ? resendDto.residentFullName()
                : originalRequest.getResidentFullName();
        String residentPhone = resendDto.residentPhone() != null && !resendDto.residentPhone().isBlank()
                ? resendDto.residentPhone()
                : originalRequest.getResidentPhone();
        String residentEmail = resendDto.residentEmail() != null && !resendDto.residentEmail().isBlank()
                ? resendDto.residentEmail()
                : originalRequest.getResidentEmail();
        String residentNationalId = resendDto.residentNationalId() != null && !resendDto.residentNationalId().isBlank()
                ? resendDto.residentNationalId()
                : originalRequest.getResidentNationalId();
        java.time.LocalDate residentDob = resendDto.residentDob() != null
                ? resendDto.residentDob()
                : originalRequest.getResidentDob();
        String relation = resendDto.relation() != null && !resendDto.relation().isBlank()
                ? resendDto.relation()
                : originalRequest.getRelation();
        String proofOfRelationImageUrl = resendDto.proofOfRelationImageUrl() != null && !resendDto.proofOfRelationImageUrl().isBlank()
                ? resendDto.proofOfRelationImageUrl()
                : originalRequest.getProofOfRelationImageUrl();
        String note = resendDto.note() != null && !resendDto.note().isBlank()
                ? resendDto.note()
                : originalRequest.getNote();

        // Create new DTO with merged values for validation
        HouseholdMemberRequestCreateDto createDto = new HouseholdMemberRequestCreateDto(
                originalRequest.getHouseholdId(),
                residentFullName,
                residentPhone,
                residentEmail,
                residentNationalId,
                residentDob,
                relation,
                proofOfRelationImageUrl,
                note
        );

        // Validate uniqueness (email, nationalId, phone) if changed
        validateEmailUniqueness(createDto, requesterResident, principal);
        validateNationalIdUniqueness(createDto);
        validatePhoneUniqueness(createDto);

        // IMPORTANT: Check for existing pending requests BEFORE creating new one
        // This prevents multiple resends of the same rejected request
        // Check based on original request's resident info first (before resolving new resident)
        boolean hasPendingRequest = false;
        
        // First, check if there's already a PENDING request for the same household + resident info
        // (This prevents resending the same rejected request multiple times)
        if (originalRequest.getResidentId() != null) {
            Optional<HouseholdMemberRequest> existingPending = requestRepository
                    .findFirstByHouseholdIdAndResidentIdAndStatusIn(
                            originalRequest.getHouseholdId(),
                            originalRequest.getResidentId(),
                            List.of(RequestStatus.PENDING)
                    );
            if (existingPending.isPresent() && !existingPending.get().getId().equals(originalRequest.getId())) {
                hasPendingRequest = true;
            }
        }
        
        // Also check by national ID if available
        if (!hasPendingRequest && originalRequest.getResidentNationalId() != null 
                && !originalRequest.getResidentNationalId().isBlank()) {
            Optional<HouseholdMemberRequest> existingPending = requestRepository
                    .findFirstByHouseholdIdAndResidentNationalIdAndStatusIn(
                            originalRequest.getHouseholdId(),
                            originalRequest.getResidentNationalId(),
                            List.of(RequestStatus.PENDING)
                    );
            if (existingPending.isPresent() && !existingPending.get().getId().equals(originalRequest.getId())) {
                hasPendingRequest = true;
            }
        }
        
        // Also check by name + phone if available
        if (!hasPendingRequest && originalRequest.getResidentPhone() != null 
                && !originalRequest.getResidentPhone().isBlank()
                && originalRequest.getResidentFullName() != null
                && !originalRequest.getResidentFullName().isBlank()) {
            Optional<HouseholdMemberRequest> existingPending = requestRepository
                    .findFirstByHouseholdIdAndResidentFullNameIgnoreCaseAndResidentPhoneAndStatusIn(
                            originalRequest.getHouseholdId(),
                            originalRequest.getResidentFullName(),
                            originalRequest.getResidentPhone(),
                            List.of(RequestStatus.PENDING)
                    );
            if (existingPending.isPresent() && !existingPending.get().getId().equals(originalRequest.getId())) {
                hasPendingRequest = true;
            }
        }
        
        if (hasPendingRequest) {
            throw new IllegalArgumentException("Không thể gửi yêu cầu này vì đã có yêu cầu đang chờ duyệt cho thành viên này");
        }

        // Check for existing pending requests with the NEW/updated information
        UUID resolvedResidentId = resolveExistingResident(createDto);
        if (resolvedResidentId != null) {
            requestRepository.findFirstByHouseholdIdAndResidentIdAndStatusIn(
                    originalRequest.getHouseholdId(),
                    resolvedResidentId,
                    List.of(RequestStatus.PENDING)
            ).ifPresent(existing -> {
                throw new IllegalArgumentException("Không thể gửi yêu cầu này vì đã có yêu cầu đang chờ duyệt cho thành viên này");
            });

            householdMemberRepository.findActiveMemberByResidentAndHousehold(resolvedResidentId, originalRequest.getHouseholdId())
                    .ifPresent(member -> {
                        throw new IllegalArgumentException("Thành viên đã là thành viên của căn hộ này");
                    });
        } else if (residentPhone != null && !residentPhone.isBlank()) {
            requestRepository.findFirstByHouseholdIdAndResidentFullNameIgnoreCaseAndResidentPhoneAndStatusIn(
                    originalRequest.getHouseholdId(),
                    residentFullName,
                    residentPhone,
                    List.of(RequestStatus.PENDING)
            ).ifPresent(existing -> {
                throw new IllegalArgumentException("Không thể gửi yêu cầu này vì đã có yêu cầu đang chờ duyệt cho thành viên này");
            });
        }

        ensureHouseholdHasCapacity(household);

        // Create new request with PENDING status
        HouseholdMemberRequest newRequest = HouseholdMemberRequest.builder()
                .householdId(originalRequest.getHouseholdId())
                .residentId(resolvedResidentId)
                .requestedBy(principal.uid())
                .residentFullName(residentFullName)
                .residentPhone(residentPhone)
                .residentEmail(residentEmail)
                .residentNationalId(residentNationalId)
                .residentDob(residentDob)
                .relation(relation)
                .proofOfRelationImageUrl(proofOfRelationImageUrl)
                .note(note)
                .status(RequestStatus.PENDING)
                .build();

        HouseholdMemberRequest saved = requestRepository.save(newRequest);
        log.info("Household member request {} resent by {} (original request: {})", saved.getId(), principal.uid(), requestId);

        return toDto(saved);
    }

    private void approveRequest(HouseholdMemberRequest request, UUID adminUserId) {
        householdRepository.findById(request.getHouseholdId())
                .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        UUID residentId = ensureResidentExists(request);

        householdMemberRepository.findActiveMemberByResidentAndHousehold(residentId, request.getHouseholdId())
                .ifPresent(member -> {
                    throw new IllegalArgumentException("Resident is already a member of this household");
                });

        houseHoldMemberService.createHouseholdMember(new HouseholdMemberCreateDto(
                request.getHouseholdId(),
                residentId,
                request.getRelation(),
                request.getProofOfRelationImageUrl(),
                Boolean.FALSE,
                null
        ));

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(adminUserId);
        request.setApprovedAt(OffsetDateTime.now());
        request.setUpdatedAt(OffsetDateTime.now());
        log.info("Household member request {} approved by {}", request.getId(), adminUserId);

        // Get building name for notification message
        String buildingName = null;
        Household household = householdRepository.findById(request.getHouseholdId()).orElse(null);
        if (household != null && household.getUnitId() != null) {
            Unit unit = unitRepository.findById(household.getUnitId()).orElse(null);
            if (unit != null && unit.getBuilding() != null) {
                buildingName = unit.getBuilding().getName();
            }
        }
        
        String message = String.format("Thành viên %s đã được thêm vào căn hộ.",
                request.getResidentFullName() != null ? request.getResidentFullName() : "");
        if (buildingName != null && !buildingName.isBlank()) {
            message += " Tòa nhà: " + buildingName;
        }
        
        notifyRequester(
                request,
                "Yêu cầu đăng ký thành viên đã được duyệt",
                message
        );
    }

    private void rejectRequest(HouseholdMemberRequest request, UUID adminUserId, String rejectionReason) {
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectedBy(adminUserId);
        request.setRejectedAt(OffsetDateTime.now());
        request.setRejectionReason(rejectionReason);
        request.setUpdatedAt(OffsetDateTime.now());
        log.info("Household member request {} rejected by {}", request.getId(), adminUserId);

        // Get building name for notification message
        String buildingName = null;
        Household household = householdRepository.findById(request.getHouseholdId()).orElse(null);
        if (household != null && household.getUnitId() != null) {
            Unit unit = unitRepository.findById(household.getUnitId()).orElse(null);
            if (unit != null && unit.getBuilding() != null) {
                buildingName = unit.getBuilding().getName();
            }
        }
        
        String message = rejectionReason != null && !rejectionReason.isBlank()
                ? rejectionReason
                : "Ban quản trị đã từ chối yêu cầu đăng ký thành viên.";
        if (buildingName != null && !buildingName.isBlank()) {
            message += " Tòa nhà: " + buildingName;
        }

        notifyRequester(
                request,
                "Yêu cầu đăng ký thành viên bị từ chối",
                message
        );
    }

    private boolean isPrimaryResident(Household household, UUID residentId) {
        if (household.getPrimaryResidentId() != null && household.getPrimaryResidentId().equals(residentId)) {
            return true;
        }

        return householdMemberRepository.findPrimaryMemberByHouseholdId(household.getId())
                .map(member -> residentId.equals(member.getResidentId()))
                .orElse(false);
    }

    private HouseholdMemberRequestDto toDto(HouseholdMemberRequest request) {
        Household household = householdRepository.findById(request.getHouseholdId()).orElse(null);
        UUID unitId = null;
        String unitCode = null;
        String householdCode = null;
        if (household != null) {
            unitId = household.getUnitId();
            householdCode = household.getId().toString();
            if (unitId != null) {
                var unit = unitRepository.findById(unitId).orElse(null);
                if (unit != null) {
                    unitCode = unit.getCode();
                    householdCode = unit.getCode();
                }
            }
        }

        Resident resident = null;
        if (request.getResidentId() != null) {
            resident = residentRepository.findById(request.getResidentId()).orElse(null);
        }
        String residentName = resident != null ? resident.getFullName() : null;
        String residentEmail = resident != null ? resident.getEmail() : null;
        String residentPhone = resident != null ? resident.getPhone() : null;

        Resident requesterResident = residentRepository.findByUserId(request.getRequestedBy()).orElse(null);
        String requestedByName = requesterResident != null ? requesterResident.getFullName() : null;

        String approvedByName = null;
        if (request.getApprovedBy() != null) {
            Resident approved = residentRepository.findByUserId(request.getApprovedBy()).orElse(null);
            approvedByName = approved != null ? approved.getFullName() : null;
        }

        String rejectedByName = null;
        if (request.getRejectedBy() != null) {
            Resident rejected = residentRepository.findByUserId(request.getRejectedBy()).orElse(null);
            rejectedByName = rejected != null ? rejected.getFullName() : null;
        }

        return new HouseholdMemberRequestDto(
                request.getId(),
                request.getHouseholdId(),
                householdCode,
                unitId,
                unitCode,
                request.getResidentId(),
                residentName,
                residentEmail,
                residentPhone,
                request.getResidentFullName(),
                request.getResidentPhone(),
                request.getResidentEmail(),
                request.getResidentNationalId(),
                request.getResidentDob(),
                request.getRequestedBy(),
                requestedByName,
                request.getRelation(),
                request.getProofOfRelationImageUrl(),
                request.getNote(),
                request.getStatus(),
                request.getApprovedBy(),
                approvedByName,
                request.getRejectedBy(),
                rejectedByName,
                request.getRejectionReason(),
                request.getApprovedAt(),
                request.getRejectedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private UUID resolveExistingResident(HouseholdMemberRequestCreateDto createDto) {
        if (createDto.residentNationalId() != null && !createDto.residentNationalId().isBlank()) {
            return residentRepository.findByNationalId(createDto.residentNationalId())
                    .map(Resident::getId)
                    .orElse(null);
        }
        if (createDto.residentPhone() != null && !createDto.residentPhone().isBlank()) {
            return residentRepository.findByPhone(createDto.residentPhone())
                    .map(Resident::getId)
                    .orElse(null);
        }
        if (createDto.residentEmail() != null && !createDto.residentEmail().isBlank()) {
            return residentRepository.findByEmail(createDto.residentEmail())
                    .map(Resident::getId)
                    .orElse(null);
        }
        return null;
    }

    private UUID ensureResidentExists(HouseholdMemberRequest request) {
        if (request.getResidentId() != null) {
            return residentRepository.findById(request.getResidentId())
                    .map(Resident::getId)
                    .orElseThrow(() -> new IllegalArgumentException("Resident not found"));
        }

        Resident existing = null;
        if (request.getResidentNationalId() != null && !request.getResidentNationalId().isBlank()) {
            existing = residentRepository.findByNationalId(request.getResidentNationalId()).orElse(null);
        }
        if (existing == null && request.getResidentPhone() != null && !request.getResidentPhone().isBlank()) {
            existing = residentRepository.findByPhone(request.getResidentPhone()).orElse(null);
        }
        if (existing == null && request.getResidentEmail() != null && !request.getResidentEmail().isBlank()) {
            existing = residentRepository.findByEmail(request.getResidentEmail()).orElse(null);
        }

        if (existing == null) {
            Resident newResident = Resident.builder()
                    .fullName(request.getResidentFullName())
                    .phone(request.getResidentPhone())
                    .email(request.getResidentEmail())
                    .nationalId(request.getResidentNationalId())
                    .dob(request.getResidentDob())
                    .build();
            existing = residentRepository.save(newResident);
            log.info("Created resident {} from household member request {}", existing.getId(), request.getId());
        }

        request.setResidentId(existing.getId());
        return existing.getId();
    }

    private void ensureHouseholdHasCapacity(Household household) {
        Unit unit = unitRepository.findById(household.getUnitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found for this household"));

        long activeMembers = householdMemberRepository.countActiveMembersByHouseholdId(household.getId());
        long pendingRequests = requestRepository.countByHouseholdIdAndStatus(household.getId(), RequestStatus.PENDING);
        int capacity = calculateCapacity(unit);

        if (activeMembers + pendingRequests >= capacity) {
            String unitLabel = unit.getCode() != null ? unit.getCode() : "Căn hộ";
            throw new IllegalArgumentException(String.format(
                    "%s đã đủ %d thành viên (bao gồm các yêu cầu đang chờ duyệt). Vui lòng chờ hoàn tất hoặc cập nhật danh sách trước khi đăng ký thêm.",
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

    private void validateEmailUniqueness(HouseholdMemberRequestCreateDto createDto,
                                         Resident requesterResident,
                                         UserPrincipal principal) {
        if (!StringUtils.hasText(createDto.residentEmail())) {
            return;
        }

        String normalizedEmail = createDto.residentEmail().trim();
        if (normalizedEmail.isEmpty()) {
            return;
        }

        String lowerEmail = normalizedEmail.toLowerCase();

        if (requesterResident != null && StringUtils.hasText(requesterResident.getEmail())) {
            String residentEmail = requesterResident.getEmail().trim().toLowerCase();
            if (!residentEmail.isEmpty() && lowerEmail.equals(residentEmail)) {
                throw new IllegalArgumentException("Email này trùng với email tài khoản của bạn. Vui lòng nhập email khác hoặc để trống.");
            }
        }

        String accountEmail = resolvePrincipalEmail(principal);
        if (accountEmail != null && lowerEmail.equals(accountEmail)) {
            throw new IllegalArgumentException("Email này trùng với email tài khoản của bạn. Vui lòng nhập email khác hoặc để trống.");
        }

        boolean existsInResidents = residentRepository.existsByEmailIgnoreCase(normalizedEmail);
        boolean existsInAccounts = iamClientService.emailExists(normalizedEmail);

        if (existsInResidents || existsInAccounts) {
            throw new IllegalArgumentException("Email này đã được sử dụng trong hệ thống. Vui lòng nhập email khác hoặc để trống.");
        }
    }

    private void validateNationalIdUniqueness(HouseholdMemberRequestCreateDto createDto) {
        if (!StringUtils.hasText(createDto.residentNationalId())) {
            return;
        }

        String normalizedId = createDto.residentNationalId().trim();
        if (normalizedId.isEmpty()) {
            return;
        }

        // Validate: không được có dấu cách
        if (normalizedId.contains(" ")) {
            throw new IllegalArgumentException("CCCD không được chứa dấu cách");
        }

        // Validate: không được có ký tự đặc biệt (chỉ cho phép số)
        if (!normalizedId.matches("^[0-9]+$")) {
            throw new IllegalArgumentException("CCCD chỉ được chứa chữ số, không được có ký tự đặc biệt");
        }

        // Validate: phải có đúng 12 chữ số
        if (!normalizedId.matches("^[0-9]{12}$")) {
            throw new IllegalArgumentException("CCCD phải có đúng 12 chữ số");
        }

        // Validate: không trùng với CCCD khác trong database
        if (residentRepository.existsByNationalId(normalizedId)) {
            throw new IllegalArgumentException("CCCD/CMND này đã tồn tại trong hệ thống. Vui lòng nhập số khác hoặc để trống.");
        }
    }

    private void validatePhoneUniqueness(HouseholdMemberRequestCreateDto createDto) {
        if (!StringUtils.hasText(createDto.residentPhone())) {
            return;
        }

        String normalizedPhone = createDto.residentPhone().trim();
        if (normalizedPhone.isEmpty()) {
            return;
        }

        // Validate: không được có dấu cách
        if (normalizedPhone.contains(" ")) {
            throw new IllegalArgumentException("Số điện thoại không được chứa dấu cách");
        }

        // Validate: không được có ký tự đặc biệt (chỉ cho phép số)
        if (!normalizedPhone.matches("^[0-9]+$")) {
            throw new IllegalArgumentException("Số điện thoại chỉ được chứa chữ số, không được có ký tự đặc biệt");
        }

        // Validate: phải có đúng 10 số và bắt đầu từ số 0
        if (!normalizedPhone.matches("^0[0-9]{9}$")) {
            throw new IllegalArgumentException("Số điện thoại phải có đúng 10 số và bắt đầu từ số 0");
        }

        if (residentRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Số điện thoại này đã tồn tại trong hệ thống. Vui lòng nhập số khác hoặc để trống.");
        }
    }

    private String resolvePrincipalEmail(UserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        try {
            ResidentAccountDto account = iamClientService.getUserAccountInfo(principal.uid());
            if (account != null && StringUtils.hasText(account.email())) {
                return account.email().trim().toLowerCase();
            }
        } catch (Exception e) {
            log.warn("Không thể lấy thông tin email tài khoản hiện tại: {}", e.getMessage());
        }
        return null;
    }

    private void notifyRequester(HouseholdMemberRequest request, String title, String message) {
        try {
            Resident requester = residentRepository.findByUserId(request.getRequestedBy()).orElse(null);
            if (requester == null || requester.getId() == null) {
                log.warn("⚠️ [HouseholdMemberRequestService] Cannot notify requester {}, resident not found", request.getRequestedBy());
                return;
            }

            UUID residentId = requester.getId();
            UUID buildingId = null;
            UUID unitId = null;
            String unitCode = null;
            String buildingName = null;

            Household household = householdRepository.findById(request.getHouseholdId()).orElse(null);
            if (household != null && household.getUnitId() != null) {
                Unit unit = unitRepository.findById(household.getUnitId()).orElse(null);
                if (unit != null) {
                    unitId = unit.getId();
                    unitCode = unit.getCode();
                    if (unit.getBuilding() != null) {
                        buildingId = unit.getBuilding().getId();
                        buildingName = unit.getBuilding().getName();
                    }
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("requestId", request.getId().toString());
            data.put("status", request.getStatus().name());
            if (unitId != null) {
                data.put("unitId", unitId.toString());
            }
            if (unitCode != null) {
                data.put("unitCode", unitCode);
            }
            if (buildingName != null) {
                data.put("buildingName", buildingName);
            }
            if (request.getResidentFullName() != null) {
                data.put("memberName", request.getResidentFullName());
            }
            if (request.getRelation() != null) {
                data.put("relation", request.getRelation());
            }
            if (request.getRejectionReason() != null) {
                data.put("reason", request.getRejectionReason());
            }

            notificationClient.sendResidentNotification(
                    residentId,
                    buildingId,
                    "REQUEST",
                    title,
                    message,
                    request.getId(),
                    "HOUSEHOLD_MEMBER_REQUEST",
                    data
            );
        } catch (Exception ex) {
            log.warn("⚠️ [HouseholdMemberRequestService] Failed to dispatch notification for request {}: {}", request.getId(), ex.getMessage());
        }
    }
}
