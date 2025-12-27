package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MaintenanceRequestConfigDto;
import com.QhomeBase.baseservice.model.MaintenanceRequest;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.MaintenanceRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class MaintenanceRequestMonitor {

    private static final String STATUS_PENDING = "PENDING";
    private static final String ADMIN_PHONE = "0984000036";
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Duration reminderThreshold; // 30 phút
    private final Duration callThreshold; // 60 phút (30 phút tiếp theo)
    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final UnitRepository unitRepository;
    private final NotificationClient notificationClient;

    public MaintenanceRequestMonitor(
            MaintenanceRequestRepository maintenanceRequestRepository,
            UnitRepository unitRepository,
            NotificationClient notificationClient,
            @Value("${maintenance.request.reminder.threshold:PT30M}") Duration reminderThreshold,
            @Value("${maintenance.request.call.threshold:PT60M}") Duration callThreshold) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.unitRepository = unitRepository;
        this.notificationClient = notificationClient;
        this.reminderThreshold = reminderThreshold;
        this.callThreshold = callThreshold;
    }

    public MaintenanceRequestConfigDto getConfig() {
        return new MaintenanceRequestConfigDto(
                reminderThreshold,
                callThreshold,
                ADMIN_PHONE
        );
    }

    @Scheduled(fixedDelayString = "${maintenance.request.monitor.delay:60000}")
    public void checkPendingRequests() {
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_TIMEZONE);
        
        // Check for reminder: requests created more than reminderThreshold ago (30 phút)
        // CHỈ áp dụng cho requests có preferredDatetime trong ngày hôm nay (realtime check)
        OffsetDateTime reminderDeadline = now.minus(reminderThreshold);
        List<MaintenanceRequest> reminderCandidates =
                maintenanceRequestRepository.findPendingRequestsForReminder(STATUS_PENDING, reminderDeadline, now);
        reminderCandidates.forEach(this::notifyResendReminder);
        maintenanceRequestRepository.saveAll(reminderCandidates);

        // Check for call alert: requests that were reminded more than callThreshold ago (60 phút total)
        // CHỈ áp dụng cho requests có preferredDatetime trong ngày hôm nay (realtime check)
        OffsetDateTime callDeadline = now.minus(callThreshold);
        List<MaintenanceRequest> callAlertCandidates =
                maintenanceRequestRepository.findPendingRequestsForCallAlert(STATUS_PENDING, callDeadline, now);
        callAlertCandidates.forEach(this::notifyCallAdmin);
        maintenanceRequestRepository.saveAll(callAlertCandidates);
    }

    private void notifyResendReminder(MaintenanceRequest request) {
        request.setResendAlertSent(true);

        long minutes = reminderThreshold.toMinutes();
        sendResidentNotification(
                request,
                "Admin chưa phản hồi yêu cầu sửa chữa",
                "Yêu cầu sửa chữa \"" + request.getTitle() + "\" đã quá " + minutes + " phút, hãy gửi lại yêu cầu để admin xem lại.",
                "RESEND_REMINDER"
        );
    }

    private void notifyCallAdmin(MaintenanceRequest request) {
        request.setCallAlertSent(true);

        long minutes = callThreshold.toMinutes();
        sendResidentNotification(
                request,
                "Cần liên hệ admin ngay",
                "Yêu cầu sửa chữa \"" + request.getTitle() + "\" đã quá " + minutes + " phút mà admin chưa phản hồi. Vui lòng gọi ngay cho admin: " + ADMIN_PHONE,
                "CALL_ADMIN_REMINDER"
        );
    }

    private void sendResidentNotification(MaintenanceRequest request, String title, String message, String status) {
        if (request.getResidentId() == null) {
            log.warn("Missing residentId for maintenance request {}", request.getId());
            return;
        }

        Unit unit = unitRepository.findById(request.getUnitId()).orElse(null);
        UUID buildingId = unit != null && unit.getBuilding() != null ? unit.getBuilding().getId() : null;

        Map<String, String> data = new HashMap<>();
        data.put("entity", "MAINTENANCE_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", status);

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                buildingId,
                "REQUEST",
                title,
                message,
                request.getId(),
                "MAINTENANCE_REQUEST",
                data
        );
    }
}

