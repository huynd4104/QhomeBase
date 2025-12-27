package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.AdminServiceRequestActionDto;
import com.QhomeBase.baseservice.dto.CleaningRequestDto;
import com.QhomeBase.baseservice.dto.CreateCleaningRequestDto;
import com.QhomeBase.baseservice.model.CleaningRequest;
import com.QhomeBase.baseservice.model.Household;
import com.QhomeBase.baseservice.model.HouseholdMember;
import com.QhomeBase.baseservice.model.Resident;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.CleaningRequestRepository;
import com.QhomeBase.baseservice.repository.HouseholdMemberRepository;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class CleaningRequestService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, BigDecimal> DEFAULT_DURATIONS = Map.of(
            "Dọn dẹp cơ bản", BigDecimal.valueOf(1),
            "Dọn dẹp tổng thể", BigDecimal.valueOf(2),
            "Dọn bếp", BigDecimal.valueOf(1.5),
            "Dọn phòng khách", BigDecimal.valueOf(1.5),
            "Giặt rèm", BigDecimal.valueOf(2),
            "Khử mùi toàn căn", BigDecimal.valueOf(1.5),
            "Vệ sinh thiết bị", BigDecimal.valueOf(1)
    );

    private final LocalTime workingStart;
    private final LocalTime workingEnd;

    private final CleaningRequestRepository cleaningRequestRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final NotificationClient notificationClient;

    public CleaningRequestService(
            CleaningRequestRepository cleaningRequestRepository,
            UnitRepository unitRepository,
            ResidentRepository residentRepository,
            HouseholdRepository householdRepository,
            HouseholdMemberRepository householdMemberRepository,
            NotificationClient notificationClient,
            @Value("${cleaning.request.working.hours.start:08:00}") String workingStartStr,
            @Value("${cleaning.request.working.hours.end:18:00}") String workingEndStr) {
        this.cleaningRequestRepository = cleaningRequestRepository;
        this.unitRepository = unitRepository;
        this.residentRepository = residentRepository;
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.notificationClient = notificationClient;
        this.workingStart = LocalTime.parse(workingStartStr, TIME_FORMATTER);
        this.workingEnd = LocalTime.parse(workingEndStr, TIME_FORMATTER);
    }

    @SuppressWarnings("null")
    public CleaningRequestDto create(UUID userId, CreateCleaningRequestDto dto) {
        Unit unit = unitRepository.findById(dto.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found"));

        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));

        Household household = householdRepository.findCurrentHouseholdByUnitId(unit.getId())
                .orElseThrow(() -> new IllegalArgumentException("Unit has no active household"));

        if (!isResidentInHousehold(resident, household)) {
            throw new IllegalArgumentException("You are not associated with this unit");
        }

        ensureNoActiveRequest(resident.getId());

        // Validate working hours for startTime
        if (dto.startTime() != null) {
            validateStartTime(dto.startTime());
        }

        String normalizedType = dto.cleaningType().trim();
        BigDecimal duration = DEFAULT_DURATIONS.getOrDefault(
                normalizedType,
                dto.durationHours().setScale(2, RoundingMode.HALF_UP)
        ).setScale(2, RoundingMode.HALF_UP);
        if (duration.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }

        String contactPhone = StringUtils.hasText(dto.contactPhone())
                ? dto.contactPhone().trim()
                : (resident.getPhone() != null ? resident.getPhone() : "");

        if (!StringUtils.hasText(contactPhone)) {
            throw new IllegalArgumentException("Contact phone is required");
        }

        CleaningRequest request = CleaningRequest.builder()
                .id(UUID.randomUUID())
                .unitId(unit.getId())
                .residentId(resident.getId())
                .createdBy(userId)
                .userId(userId)
                .cleaningType(normalizedType)
                .cleaningDate(dto.cleaningDate())
                .startTime(dto.startTime())
                .durationHours(duration)
                .location(dto.location().trim())
                .note(StringUtils.hasText(dto.note()) ? dto.note().trim() : null)
                .contactPhone(contactPhone)
                .extraServices(dto.extraServices())
                .paymentMethod(StringUtils.hasText(dto.paymentMethod()) ? dto.paymentMethod().trim() : null)
                .status(STATUS_PENDING)
                .build();

        CleaningRequest saved = cleaningRequestRepository.save(request);
        log.info("Created cleaning request {} for unit {}", saved.getId(), saved.getUnitId());
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public List<CleaningRequestDto> getMyRequests(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        List<CleaningRequest> requests = cleaningRequestRepository
                .findByResidentIdOrderByCreatedAtDesc(resident.getId());
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get paginated requests for a resident (all statuses)
     */
    public Map<String, Object> getMyRequestsPaged(UUID userId, int limit, int offset) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        List<CleaningRequest> requests = cleaningRequestRepository
                .findByResidentIdWithPagination(resident.getId(), limit, offset);
        
        long total = cleaningRequestRepository.countByResidentId(resident.getId());
        
        List<CleaningRequestDto> dtos = requests.stream()
                .map(this::toDto)
                .toList();
        
        return Map.of(
                "requests", dtos,
                "total", total,
                "limit", limit,
                "offset", offset
        );
    }

    /**
     * Get completed (paid) cleaning requests for a resident
     * Note: Cleaning requests don't have paymentStatus, so we use status = DONE
     */
    public List<CleaningRequestDto> getPaidRequests(UUID userId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        List<CleaningRequest> requests = cleaningRequestRepository
                .findByResidentIdAndStatusDone(resident.getId());
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<CleaningRequestDto> getPendingRequests() {
        List<CleaningRequest> requests = cleaningRequestRepository
                .findByStatusOrderByCreatedAtAsc(STATUS_PENDING);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    private void validateStartTime(LocalTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time is required");
        }

        if (startTime.isBefore(workingStart) || startTime.isAfter(workingEnd)) {
            throw new IllegalArgumentException(
                    String.format("Start time must be between %s and %s",
                            workingStart.format(TIME_FORMATTER), workingEnd.format(TIME_FORMATTER)));
        }
    }

    private boolean isResidentInHousehold(Resident resident, Household household) {
        if (resident == null || household == null) {
            return false;
        }
        if (household.getPrimaryResidentId() != null &&
                household.getPrimaryResidentId().equals(resident.getId())) {
            return true;
        }

        List<HouseholdMember> members = householdMemberRepository
                .findActiveMembersByHouseholdId(household.getId());
        return members.stream()
                .anyMatch(member -> resident.getId().equals(member.getResidentId()));
    }

    private CleaningRequestDto toDto(CleaningRequest entity) {
        return new CleaningRequestDto(
                entity.getId(),
                entity.getUnitId(),
                entity.getResidentId(),
                entity.getCreatedBy(),
                entity.getUserId(),
                entity.getCleaningType(),
                entity.getCleaningDate(),
                entity.getStartTime(),
                entity.getDurationHours(),
                entity.getLocation(),
                entity.getNote(),
                entity.getContactPhone(),
                entity.getExtraServices(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastResentAt(),
                entity.isResendAlertSent()
        );
    }

    @SuppressWarnings("null")
    public CleaningRequestDto approveRequest(UUID adminId, UUID requestId, AdminServiceRequestActionDto dto) {
        CleaningRequest request = cleaningRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaning request not found"));

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be moved to in-progress");
        }

        request.setStatus(STATUS_IN_PROGRESS);
        CleaningRequest saved = cleaningRequestRepository.save(request);
        notifyCleaningInProgress(saved, dto != null ? dto.note() : null);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CleaningRequestDto completeRequest(UUID staffId, UUID requestId, AdminServiceRequestActionDto dto) {
        CleaningRequest request = cleaningRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaning request not found"));

        if (!STATUS_IN_PROGRESS.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only in-progress requests can be marked done");
        }

        request.setStatus(STATUS_DONE);
        CleaningRequest saved = cleaningRequestRepository.save(request);
        notifyCleaningCompleted(saved, dto != null ? dto.note() : null);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CleaningRequestDto resendRequest(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        CleaningRequest request = cleaningRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaning request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only resend your own requests");
        }

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending requests can be resent");
        }

        OffsetDateTime now = OffsetDateTime.now();
        request.setLastResentAt(now);
        request.setResendAlertSent(false);
        CleaningRequest saved = cleaningRequestRepository.save(request);
        notifyCleaningResendPrompt(saved);
        return toDto(saved);
    }

    @SuppressWarnings("null")
    public CleaningRequestDto cancelRequest(UUID userId, UUID requestId) {
        Resident resident = residentRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Resident profile not found"));
        
        CleaningRequest request = cleaningRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Cleaning request not found"));

        if (request.getResidentId() == null || !request.getResidentId().equals(resident.getId())) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        if (STATUS_DONE.equalsIgnoreCase(request.getStatus()) ||
                STATUS_CANCELLED.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Cannot cancel a completed or already cancelled request");
        }

        request.setStatus(STATUS_CANCELLED);
        request.setResendAlertSent(false);
        CleaningRequest saved = cleaningRequestRepository.save(request);
        // Không gửi notification khi cư dân tự hủy request
        // notifyCleaningCancelled(saved);
        return toDto(saved);
    }

    private void ensureNoActiveRequest(UUID residentId) {
        if (residentId == null) {
            return;
        }
        boolean hasPending = cleaningRequestRepository
                .existsByResidentIdAndStatusIgnoreCase(residentId, STATUS_PENDING);
        boolean hasInProgress = cleaningRequestRepository
                .existsByResidentIdAndStatusIgnoreCase(residentId, STATUS_IN_PROGRESS);
        if (hasPending || hasInProgress) {
            throw new IllegalStateException(
                    "Bạn đang có yêu cầu dọn dẹp chưa hoàn tất. Vui lòng chờ đơn hiện tại sang trạng thái DONE trước khi tạo thêm.");
        }
    }

    private void notifyCleaningInProgress(CleaningRequest request, String note) {
        StringBuilder body = new StringBuilder("Yêu cầu dọn dẹp \"")
                .append(request.getCleaningType())
                .append("\" đang được xử lý.");
        if (note != null && !note.isBlank()) {
            body.append(' ').append(note.trim());
        } else {
            body.append(" Nhân viên sẽ liên hệ để sắp xếp lịch phù hợp.");
        }

        sendCleaningNotification(
                request,
                "Yêu cầu dọn dẹp đang xử lý",
                body.toString(),
                STATUS_IN_PROGRESS
        );
    }

    private void notifyCleaningCompleted(CleaningRequest request, String note) {
        StringBuilder body = new StringBuilder("Yêu cầu dọn dẹp \"")
                .append(request.getCleaningType())
                .append("\" đã hoàn tất.");
        if (note != null && !note.isBlank()) {
            body.append(' ').append(note.trim());
        }

        sendCleaningNotification(
                request,
                "Yêu cầu dọn dẹp đã hoàn tất",
                body.toString(),
                STATUS_DONE
        );
    }

    private void notifyCleaningResendPrompt(CleaningRequest request) {
        StringBuilder body = new StringBuilder("Yêu cầu dọn dẹp \"")
                .append(request.getCleaningType())
                .append("\" của bạn đã được gửi lại.");

        sendCleaningNotification(
                request,
                "Yêu cầu dọn dẹp đã được gửi lại",
                body.toString(),
                STATUS_PENDING
        );
    }

    private void notifyCleaningCancelled(CleaningRequest request) {
        sendCleaningNotification(
                request,
                "Yêu cầu dọn dẹp đã được hủy",
                "Yêu cầu dọn dẹp \"" + request.getCleaningType() + "\" đã được hủy. Nếu cần, bạn có thể gửi lại yêu cầu mới.",
                STATUS_CANCELLED
        );
    }

    private void sendCleaningNotification(
            CleaningRequest request,
            String title,
            String body,
            String status
    ) {
        if (request.getResidentId() == null) {
            log.warn("⚠️ [CleaningRequest] Missing residentId for request {}", request.getId());
            return;
        }

        Unit unit = null;
        UUID unitId = request.getUnitId();
        if (unitId != null) {
            unit = unitRepository.findById(unitId).orElse(null);
        }

        Map<String, String> data = new HashMap<>();
        data.put("entity", "CLEANING_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", status);
        data.put("location", request.getLocation());

        UUID buildingId = (unit != null && unit.getBuilding() != null)
                ? unit.getBuilding().getId()
                : null;

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                buildingId,
                "REQUEST",
                title,
                body,
                request.getId(),
                "CLEANING_REQUEST",
                data
        );
    }
}

