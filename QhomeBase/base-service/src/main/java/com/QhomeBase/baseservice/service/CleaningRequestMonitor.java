package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.CleaningRequestConfigDto;
import com.QhomeBase.baseservice.model.CleaningRequest;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.CleaningRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class CleaningRequestMonitor {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Duration reminderThreshold;
    private final Duration resendCancelThreshold;
    private final Duration noResendCancelThreshold;
    private final CleaningRequestRepository cleaningRequestRepository;
    private final UnitRepository unitRepository;
    private final NotificationClient notificationClient;

    public CleaningRequestMonitor(
            CleaningRequestRepository cleaningRequestRepository,
            UnitRepository unitRepository,
            NotificationClient notificationClient,
            @Value("${cleaning.request.reminder.threshold:PT5M}") Duration reminderThreshold,
            @Value("${cleaning.request.resend.cancel.threshold:PT5M}") Duration resendCancelThreshold,
            @Value("${cleaning.request.no.resend.cancel.threshold:PT6M}") Duration noResendCancelThreshold) {
        this.cleaningRequestRepository = cleaningRequestRepository;
        this.unitRepository = unitRepository;
        this.notificationClient = notificationClient;
        this.reminderThreshold = reminderThreshold;
        this.resendCancelThreshold = resendCancelThreshold;
        this.noResendCancelThreshold = noResendCancelThreshold;
    }

    public CleaningRequestConfigDto getConfig() {
        return new CleaningRequestConfigDto(
                reminderThreshold,
                resendCancelThreshold,
                noResendCancelThreshold
        );
    }

    @Scheduled(fixedDelayString = "${cleaning.request.monitor.delay:60000}")
    public void checkPendingRequests() {
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_TIMEZONE);
        LocalDate today = now.toLocalDate();
        
        // Check for reminder: requests created more than reminderThreshold ago
        // CHỈ áp dụng cho requests có cleaningDate trong ngày hôm nay (realtime check)
        OffsetDateTime reminderDeadline = now.minus(reminderThreshold);
        List<CleaningRequest> reminderCandidates =
                cleaningRequestRepository.findPendingRequestsForReminder(STATUS_PENDING, reminderDeadline, today);
        reminderCandidates.forEach(this::notifyResendReminder);
        cleaningRequestRepository.saveAll(reminderCandidates);

        // Check for auto-cancel: requests that were resent more than resendCancelThreshold ago
        // CHỈ áp dụng cho requests có cleaningDate trong ngày hôm nay (realtime check)
        OffsetDateTime resendCancelDeadline = now.minus(resendCancelThreshold);
        List<CleaningRequest> resentCancelCandidates =
                cleaningRequestRepository.findResentRequestsForAutoCancel(STATUS_PENDING, resendCancelDeadline, today);
        resentCancelCandidates.forEach(this::autoCancelRequest);
        cleaningRequestRepository.saveAll(resentCancelCandidates);

        // Check for auto-cancel: requests that were NOT resent but reminder was sent
        // Cancel after noResendCancelThreshold from createdAt (5 hours reminder + 1 hour grace period)
        // CHỈ áp dụng cho requests có cleaningDate trong ngày hôm nay (realtime check)
        OffsetDateTime noResendCancelDeadline = now.minus(noResendCancelThreshold);
        List<CleaningRequest> noResendCancelCandidates =
                cleaningRequestRepository.findNonResentRequestsForAutoCancel(STATUS_PENDING, noResendCancelDeadline, today);
        noResendCancelCandidates.forEach(this::autoCancelRequest);
        cleaningRequestRepository.saveAll(noResendCancelCandidates);
    }

    private void notifyResendReminder(CleaningRequest request) {
        request.setResendAlertSent(true);

        long hours = reminderThreshold.toHours();
        sendResidentNotification(
                request,
                "Admin chưa phản hồi yêu cầu dọn dẹp",
                "Yêu cầu dọn dẹp \"" + request.getCleaningType() + "\" đã quá " + hours + " tiếng, hãy gửi lại yêu cầu để admin xem lại.",
                "RESEND_REMINDER"
        );
    }

    private void autoCancelRequest(CleaningRequest request) {
        request.setStatus(STATUS_CANCELLED);
        request.setResendAlertSent(false);

        String cancelMessage;
        if (request.getLastResentAt() != null) {
            long hours = resendCancelThreshold.toHours();
            cancelMessage = "Yêu cầu dọn dẹp \"" + request.getCleaningType() + "\" đã quá " + hours + " tiếng sau khi gửi lại mà admin không phản hồi nên đã tự hủy. Bạn có thể gửi lại yêu cầu mới nếu cần.";
        } else {
            long hours = noResendCancelThreshold.toHours();
            cancelMessage = "Yêu cầu dọn dẹp \"" + request.getCleaningType() + "\" đã quá " + hours + " tiếng mà admin không phản hồi nên đã tự hủy. Bạn có thể gửi lại yêu cầu mới nếu cần.";
        }

        sendResidentNotification(
                request,
                "Yêu cầu dọn dẹp đã bị hủy",
                cancelMessage,
                "AUTO_CANCELLED"
        );
    }

    private void sendResidentNotification(CleaningRequest request, String title, String message, String status) {
        if (request.getResidentId() == null) {
            log.warn("Missing residentId for cleaning request {}", request.getId());
            return;
        }

        Unit unit = unitRepository.findById(request.getUnitId()).orElse(null);
        UUID buildingId = unit != null && unit.getBuilding() != null ? unit.getBuilding().getId() : null;

        Map<String, String> data = new HashMap<>();
        data.put("entity", "CLEANING_REQUEST");
        data.put("requestId", request.getId().toString());
        data.put("status", status);

        notificationClient.sendResidentNotification(
                request.getResidentId(),
                buildingId,
                "REQUEST",
                title,
                message,
                request.getId(),
                "CLEANING_REQUEST",
                data
        );
    }
}

