package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.VehicleActivatedEvent;
import com.QhomeBase.baseservice.dto.VehicleRegistrationApproveDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationCreateDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationRejectDto;
import com.QhomeBase.baseservice.model.VehicleRegistrationRequest;
import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;
import com.QhomeBase.baseservice.repository.VehicleRegistrationRepository;
import com.QhomeBase.baseservice.repository.VehicleRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleRegistrationService {
    private final VehicleRegistrationRepository vehicleRegistrationRepository;
    private final VehicleRepository vehicleRepository;
    private final FinanceBillingClient financeBillingClient;
    private final HouseholdService householdService;
    private final NotificationClient notificationClient;

    private OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Transactional
    public VehicleRegistrationDto createRegistrationRequest(VehicleRegistrationCreateDto dto, Authentication authentication) {
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID requestById = u.uid();
        
        if (vehicleRegistrationRepository.existsByVehicleId(dto.vehicleId())) {
            throw new IllegalStateException("Registration request for this vehicle already exists");
        }

        var request = VehicleRegistrationRequest.builder()
                .vehicle(vehicleRepository.findById(dto.vehicleId())
                        .orElseThrow())
                .reason(dto.reason())
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(requestById)
                .requestedAt(nowUTC())
                .createdAt(nowUTC())
                .updatedAt(nowUTC())
                .build();

        var savedRequest = vehicleRegistrationRepository.save(request);
        return toDto(savedRequest);
    }

    @Transactional
    public VehicleRegistrationDto approveRequest(UUID requestId, VehicleRegistrationApproveDto dto, Authentication authentication) {
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID approvedBy = u.uid();
        OffsetDateTime now = nowUTC();
        
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (!VehicleRegistrationStatus.PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Request is not PENDING. Current status: " + request.getStatus());
        }

        request.setApprovedBy(approvedBy);
        request.setNote(dto.note());
        request.setApprovedAt(now);
        request.setStatus(VehicleRegistrationStatus.APPROVED);
        request.setUpdatedAt(now);

        var savedRequest = vehicleRegistrationRepository.save(request);

        var vehicle = request.getVehicle();
        if (vehicle != null) {
            vehicle.setActivatedAt(now);
            vehicle.setRegistrationApprovedAt(now);
            vehicle.setApprovedBy(approvedBy);
            vehicle.setUpdatedAt(now);
            vehicleRepository.save(vehicle);
        }
        
        notifyVehicleActivated(savedRequest);

        return toDto(savedRequest);
    }

    @Transactional
    public VehicleRegistrationDto rejectRequest(UUID requestId, VehicleRegistrationRejectDto dto, Authentication authentication) {
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID rejectedBy = u.uid();
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (!VehicleRegistrationStatus.PENDING.equals(request.getStatus())) {
            throw new IllegalStateException("Request is not PENDING. Current status: " + request.getStatus());
        }

        request.setApprovedBy(rejectedBy);
        request.setNote(dto.reason());
        request.setApprovedAt(nowUTC());
        request.setStatus(VehicleRegistrationStatus.REJECTED);
        request.setUpdatedAt(nowUTC());

        var savedRequest = vehicleRegistrationRepository.save(request);
        
        // Send notification to resident when request is rejected
        notifyVehicleRejected(savedRequest, dto.reason());
        
        return toDto(savedRequest);
    }

    @Transactional
    public VehicleRegistrationDto cancelRequest(UUID requestId, Authentication authentication) {
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID userId = u.uid();
        
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        boolean isRequester = request.getRequestedBy().equals(userId);
        boolean hasManagerRole = u.roles() != null && 
            (u.roles().contains("manager") || 
             u.roles().contains("owner") || 
             u.roles().contains("admin"));
        
        if (!isRequester && !hasManagerRole) {
            throw new IllegalStateException("Only the requester or manager can cancel the request");
        }

        request.setStatus(VehicleRegistrationStatus.CANCELED);
        request.setUpdatedAt(nowUTC());

        var savedRequest = vehicleRegistrationRepository.save(request);
        return toDto(savedRequest);
    }

    public VehicleRegistrationDto getRequestById(UUID id) {
        var request = vehicleRegistrationRepository.findByIdWithVehicle(id);
        if (request == null) {
            throw new IllegalArgumentException("Registration request not found");
        }
        return toDto(request);
    }

    public List<VehicleRegistrationDto> getAllRequests() {
        var requests = vehicleRegistrationRepository.findAllWithVehicle();
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleRegistrationDto> getRequestsByStatus(VehicleRegistrationStatus status) {
        var requests = vehicleRegistrationRepository.findAllByStatus(status);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleRegistrationDto> getPendingRequests() {
        return getRequestsByStatus(VehicleRegistrationStatus.PENDING);
    }

    public List<VehicleRegistrationDto> getRequestsByVehicleId(UUID vehicleId) {
        var requests = vehicleRegistrationRepository.findAllByVehicleId(vehicleId);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get pending requests by building
     */
    public List<VehicleRegistrationDto> getPendingRequestsByBuilding(UUID buildingId) {
        var requests = vehicleRegistrationRepository.findPendingByBuilding(buildingId);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Get requests by building and status
     */
    public List<VehicleRegistrationDto> getRequestsByBuildingAndStatus(
            UUID buildingId, VehicleRegistrationStatus status) {
        var requests = vehicleRegistrationRepository.findByBuildingAndStatus(
            buildingId, status
        );
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    private void notifyVehicleActivated(VehicleRegistrationRequest request) {
        OffsetDateTime now = nowUTC();

        var vehicle = request.getVehicle();
        if (vehicle == null) {
            log.warn("⚠️ [VehicleRegistration] Cannot send approval notification: vehicle is null for request {}", request.getId());
            return;
        }

        UUID unitId = vehicle.getUnit() != null ? vehicle.getUnit().getId() : null;
        
        UUID payerResidentId = null;
        if (unitId != null) {
            payerResidentId = householdService.getPayerForUnit(unitId);
        }
        
        if (payerResidentId == null) {
            payerResidentId = vehicle.getResidentId();
        }
        
        if (payerResidentId == null) {
            log.warn("⚠️ [VehicleRegistration] Cannot send approval notification: residentId is null for request {}", request.getId());
            return;
        }

        // Send to finance billing service
        var event = VehicleActivatedEvent.builder()
                .vehicleId(vehicle.getId())
                .unitId(unitId)
                .residentId(payerResidentId)
                .plateNo(vehicle.getPlateNo())
                .vehicleKind(vehicle.getKind() != null ? vehicle.getKind().name() : null)
                .activatedAt(now)
                .approvedBy(request.getApprovedBy())
                .build();

        financeBillingClient.notifyVehicleActivatedSync(event);
        
        // Send private notification to resident
        try {
            String plateNo = vehicle.getPlateNo() != null ? vehicle.getPlateNo() : "xe";
            String title = "Yêu cầu đăng ký thẻ xe đã được chấp nhận";
            String message = String.format("Yêu cầu đăng ký thẻ xe với biển số %s đã được chấp nhận. Thẻ xe của bạn đã được kích hoạt.", plateNo);

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("requestId", request.getId().toString());
            data.put("status", "APPROVED");
            data.put("plateNo", plateNo);
            if (request.getNote() != null && !request.getNote().isBlank()) {
                data.put("note", request.getNote());
            }

            // Send PRIVATE notification to resident (buildingId = null for private notification)
            notificationClient.sendResidentNotification(
                    payerResidentId,
                    null, // buildingId = null for private notification (riêng tư)
                    "REQUEST",
                    title,
                    message,
                    request.getId(),
                    "VEHICLE_REGISTRATION",
                    data
            );
            
            log.info("✅ [VehicleRegistration] Sent approval notification to resident {} for request {}", payerResidentId, request.getId());
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Failed to send approval notification for request {}: {}", request.getId(), e.getMessage(), e);
        }
    }

    private void notifyVehicleRejected(VehicleRegistrationRequest request, String reason) {
        try {
            var vehicle = request.getVehicle();
            if (vehicle == null) {
                log.warn("⚠️ [VehicleRegistration] Cannot send rejection notification: vehicle is null for request {}", request.getId());
                return;
            }

            UUID unitId = vehicle.getUnit() != null ? vehicle.getUnit().getId() : null;
            UUID residentId = null;
            
            // Get residentId: try payer first, then vehicle's residentId
            if (unitId != null) {
                residentId = householdService.getPayerForUnit(unitId);
            }
            if (residentId == null) {
                residentId = vehicle.getResidentId();
            }
            
            if (residentId == null) {
                log.warn("⚠️ [VehicleRegistration] Cannot send rejection notification: residentId is null for request {}", request.getId());
                return;
            }

            String plateNo = vehicle.getPlateNo() != null ? vehicle.getPlateNo() : "xe";
            String title = "Yêu cầu đăng ký thẻ xe bị từ chối";
            String message = reason != null && !reason.isBlank()
                    ? String.format("Yêu cầu đăng ký thẻ xe với biển số %s đã bị từ chối. Lý do: %s", plateNo, reason)
                    : String.format("Yêu cầu đăng ký thẻ xe với biển số %s đã bị từ chối. Vui lòng liên hệ quản trị viên để biết thêm chi tiết.", plateNo);

            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("requestId", request.getId().toString());
            data.put("status", "REJECTED");
            data.put("plateNo", plateNo);
            if (reason != null) {
                data.put("reason", reason);
            }

            // Send PRIVATE notification to resident (buildingId = null for private notification)
            notificationClient.sendResidentNotification(
                    residentId,
                    null, // buildingId = null for private notification (riêng tư)
                    "REQUEST",
                    title,
                    message,
                    request.getId(),
                    "VEHICLE_REGISTRATION",
                    data
            );
            
            log.info("✅ [VehicleRegistration] Sent rejection notification to resident {} for request {}", residentId, request.getId());
        } catch (Exception e) {
            log.error("❌ [VehicleRegistration] Failed to send rejection notification for request {}: {}", request.getId(), e.getMessage(), e);
        }
    }

    public VehicleRegistrationDto toDto(VehicleRegistrationRequest request) {
        UUID vehicleId;
        String vehiclePlateNo;
        String vehicleKind;
        String vehicleColor;

        try {
            if (request.getVehicle() != null) {
                vehicleId = request.getVehicle().getId();
                vehiclePlateNo = request.getVehicle().getPlateNo();
                vehicleKind = request.getVehicle().getKind() != null ? request.getVehicle().getKind().name() : null;
                vehicleColor = request.getVehicle().getColor();
            } else {
                vehicleId = null;
                vehiclePlateNo = null;
                vehicleKind = null;
                vehicleColor = null;
            }
        } catch (Exception e) {
            vehicleId = null;
            vehiclePlateNo = "Unknown";
            vehicleKind = "Unknown";
            vehicleColor = "Unknown";
        }

        String requestedByName = "Unknown";
        String approvedByName = request.getApprovedBy() != null ? "Unknown" : null;

        return new VehicleRegistrationDto(
                request.getId(),
                vehicleId,
                vehiclePlateNo,
                vehicleKind,
                vehicleColor,
                request.getReason(),
                request.getStatus(),
                request.getRequestedBy(),
                requestedByName,
                request.getApprovedBy(),
                approvedByName,
                request.getNote(),
                request.getRequestedAt(),
                request.getApprovedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
