package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import com.QhomeBase.customerinteractionservice.client.IamServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdDto;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdMemberDto;
import com.QhomeBase.customerinteractionservice.client.dto.IamUserInfoResponse;
import com.QhomeBase.customerinteractionservice.client.dto.UnitDto;
import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.dto.RequestApproveRequest;
import com.QhomeBase.customerinteractionservice.dto.RequestDTO;
import com.QhomeBase.customerinteractionservice.dto.notification.CreateNotificationRequest;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.NotificationType;
import com.QhomeBase.customerinteractionservice.model.ProcessingLog;
import com.QhomeBase.customerinteractionservice.model.Request;
import com.QhomeBase.customerinteractionservice.repository.processingLogRepository;
import com.QhomeBase.customerinteractionservice.repository.requestRepository;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class processingLogService {

    private final processingLogRepository processingLogRepository;
    private final requestRepository requestRepository;
    private final IamServiceClient iamServiceClient;
    private final NotificationService notificationService;
    private final NotificationPushService notificationPushService;
    private final BaseServiceClient baseServiceClient;

    public processingLogService(processingLogRepository processingLogRepository,
                                requestRepository requestRepository,
                                IamServiceClient iamServiceClient,
                                NotificationService notificationService,
                                NotificationPushService notificationPushService,
                                BaseServiceClient baseServiceClient) {
        this.processingLogRepository = processingLogRepository;
        this.requestRepository = requestRepository;
        this.iamServiceClient = iamServiceClient;
        this.notificationService = notificationService;
        this.notificationPushService = notificationPushService;
        this.baseServiceClient = baseServiceClient;
    }

    private ProcessingLogDTO mapToDto(ProcessingLog entity) {
        String staffName = entity.getStaffInChargeName();
        String staffEmail = null;
        if (entity.getStaffInCharge() != null) {
            IamUserInfoResponse info = iamServiceClient.fetchUserInfo(entity.getStaffInCharge());
            if (info != null) {
                if (!StringUtils.hasText(staffName)) {
                    staffName = info.username();
                }
                staffEmail = info.email();
            }
        }
        return new ProcessingLogDTO(
                entity.getId(),
                entity.getRecordId(),
                entity.getContent(),
                entity.getRequestStatus(),
                staffName,
                staffEmail,
                entity.getCreatedAt().toString().replace("T", " ")
        );
    }

    public List<ProcessingLogDTO> getProcessingLogsById(UUID recordId) {
        return processingLogRepository.findByRecordIdOrderByCreatedAtDesc(recordId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<ProcessingLogDTO> getProcessingLogsByLogsId(UUID logsId) {
        return processingLogRepository.findById(logsId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<ProcessingLogDTO> getProcessingLogsByStaffId(UUID staffId) {
        return processingLogRepository.findByStaffInCharge(staffId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public ProcessingLogDTO addProcessingLog(UUID id, ProcessingLogDTO dto) {
        return addProcessingLog(id, dto, null);
    }

    @Transactional
    public ProcessingLogDTO addProcessingLog(UUID id, ProcessingLogDTO dto, Authentication authentication) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + id));

        if ("Done".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Request has been completed and cannot be updated");
        }

        String oldStatus = request.getStatus();
        String newStatus = StringUtils.hasText(dto.getRequestStatus())
                ? dto.getRequestStatus()
                : request.getStatus();
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        Request savedRequest = requestRepository.save(request);

        UUID staffId = resolveStaffId(authentication);
        String staffName = resolveStaffName(staffId);

        ProcessingLog entity = new ProcessingLog();
        entity.setRecordId(id);
        entity.setStaffInCharge(staffId);
        entity.setContent(dto.getContent());
        entity.setRequestStatus(newStatus);
        entity.setStaffInChargeName(staffName);
        entity.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        processingLogRepository.save(entity);

        // Gửi notification khi admin approve request (status chuyển từ PENDING sang APPROVED hoặc IN_PROGRESS)
        if (isPendingToApproved(oldStatus, newStatus) || isPendingToInProgress(oldStatus, newStatus)) {
            sendRequestApprovalNotification(savedRequest);
        }

        // Gửi notification khi request được giải quyết (status chuyển sang RESOLVED)
        if (isResolved(newStatus) && !isResolved(oldStatus)) {
            sendRequestResolvedNotification(savedRequest);
        }

        return mapToDto(entity);
    }

    private boolean isPendingToApproved(String oldStatus, String newStatus) {
        return ("PENDING".equalsIgnoreCase(oldStatus) || "Pending".equalsIgnoreCase(oldStatus))
                && ("APPROVED".equalsIgnoreCase(newStatus) || "Approved".equalsIgnoreCase(newStatus));
    }

    private boolean isPendingToInProgress(String oldStatus, String newStatus) {
        return ("PENDING".equalsIgnoreCase(oldStatus) || "Pending".equalsIgnoreCase(oldStatus))
                && ("IN_PROGRESS".equalsIgnoreCase(newStatus) || "In Progress".equalsIgnoreCase(newStatus));
    }

    private boolean isResolved(String status) {
        return "RESOLVED".equalsIgnoreCase(status) || "Resolved".equalsIgnoreCase(status);
    }

    private void sendRequestApprovalNotification(Request request) {
        try {
            String title = "Yêu cầu của bạn đã được duyệt";
            String message = String.format("Yêu cầu \"%s\" (Mã: %s) đã được duyệt. Đã thuê người đến xử lý.", 
                    request.getTitle(), request.getRequestCode());

            // Gửi notification trực tiếp đến residentId cụ thể (người tạo request)
            Map<String, String> dataPayload = new HashMap<>();
            dataPayload.put("notificationId", "");
            dataPayload.put("type", NotificationType.REQUEST.name());
            dataPayload.put("referenceId", request.getId().toString());
            dataPayload.put("referenceType", "REQUEST");
            dataPayload.put("scope", NotificationScope.EXTERNAL.name());

            notificationPushService.sendPushNotificationToResident(
                    request.getResidentId(),
                    title,
                    message,
                    dataPayload
            );
            log.info("✅ [Request] Đã gửi push notification đến residentId {} cho requestId: {}", 
                    request.getResidentId(), request.getId());
        } catch (Exception e) {
            log.error("❌ [Request] Không thể gửi notification approval cho requestId: {}, residentId: {}", 
                    request.getId(), request.getResidentId(), e);
        }
    }

    private void sendRequestResolvedNotification(Request request) {
        try {
            String title = "Yêu cầu của bạn đã được giải quyết";
            String message = String.format("Yêu cầu \"%s\" (Mã: %s) đã được giải quyết. Vui lòng kiểm tra và phản hồi nếu có vấn đề.", 
                    request.getTitle(), request.getRequestCode());

            // Gửi notification trực tiếp đến residentId cụ thể (người tạo request)
            Map<String, String> dataPayload = new HashMap<>();
            dataPayload.put("notificationId", "");
            dataPayload.put("type", NotificationType.REQUEST.name());
            dataPayload.put("referenceId", request.getId().toString());
            dataPayload.put("referenceType", "REQUEST");
            dataPayload.put("scope", NotificationScope.EXTERNAL.name());

            notificationPushService.sendPushNotificationToResident(
                    request.getResidentId(),
                    title,
                    message,
                    dataPayload
            );
            log.info("✅ [Request] Đã gửi push notification đến residentId {} cho requestId: {}", 
                    request.getResidentId(), request.getId());
        } catch (Exception e) {
            log.error("❌ [Request] Không thể gửi notification resolved cho requestId: {}, residentId: {}", 
                    request.getId(), request.getResidentId(), e);
        }
    }

    private UUID getBuildingIdForResident(UUID residentId) {
        try {
            // Lấy household members của resident
            List<HouseholdMemberDto> members = baseServiceClient.getActiveHouseholdMembersByResident(residentId);
            if (members == null || members.isEmpty()) {
                return null;
            }

            // Lấy household đầu tiên
            HouseholdDto household = baseServiceClient.getHouseholdById(members.get(0).householdId());
            if (household == null || household.unitId() == null) {
                return null;
            }

            // Lấy unit để lấy buildingId
            UnitDto unit = baseServiceClient.getUnitById(household.unitId());
            return unit != null ? unit.buildingId() : null;
        } catch (Exception e) {
            log.warn("⚠️ [Request] Không thể lấy buildingId cho residentId: {}", residentId, e);
            return null;
        }
    }

    @Transactional
    public RequestDTO approveRequest(UUID requestId, RequestApproveRequest approveRequest, Authentication authentication) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));

        String oldStatus = request.getStatus();
        
        // Validate request must be PENDING
        if (!"PENDING".equalsIgnoreCase(oldStatus) && !"Pending".equalsIgnoreCase(oldStatus)) {
            throw new IllegalStateException("Request is not PENDING. Current status: " + oldStatus);
        }

        // Update status to APPROVED (admin đã thấy request và thuê người tới làm)
        String newStatus = "APPROVED";
        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        Request savedRequest = requestRepository.save(request);

        // Create processing log
        UUID staffId = resolveStaffId(authentication);
        String staffName = resolveStaffName(staffId);

        String logContent = StringUtils.hasText(approveRequest.content()) 
                ? approveRequest.content()
                : "Yêu cầu đã được duyệt. Đã thuê người đến xử lý.";
        
        if (StringUtils.hasText(approveRequest.note())) {
            logContent += "\nGhi chú: " + approveRequest.note();
        }

        ProcessingLog entity = new ProcessingLog();
        entity.setRecordId(requestId);
        entity.setStaffInCharge(staffId);
        entity.setContent(logContent);
        entity.setRequestStatus(newStatus);
        entity.setStaffInChargeName(staffName);
        entity.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        processingLogRepository.save(entity);

        // Gửi notification khi admin approve request
        sendRequestApprovalNotification(savedRequest);

        // Map to DTO
        return mapRequestToDto(savedRequest);
    }

    private RequestDTO mapRequestToDto(Request request) {
        return new RequestDTO(
            request.getId(),
            request.getRequestCode(),
            request.getResidentId(),
            request.getResidentName(),
            request.getImagePath(),
            request.getTitle(),
            request.getContent(),
            request.getStatus(),
            request.getType(),
            request.getFee(),
            request.getRepairedDate() != null ? request.getRepairedDate().toString() : null,
            request.getServiceBookingId(),
            request.getCreatedAt().toString().replace("T", " "),
            request.getUpdatedAt() != null ? request.getUpdatedAt().toString().replace("T", " ") : null
        );
    }

    private UUID resolveStaffId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.uid();
        }
        return null;
    }

    private String resolveStaffName(UUID staffId) {
        if (staffId == null) {
            return null;
        }
        IamUserInfoResponse info = iamServiceClient.fetchUserInfo(staffId);
        if (info == null) {
            return null;
        }
        if (StringUtils.hasText(info.username())) {
            return info.username();
        }
        return info.email();
    }
}
