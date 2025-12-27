package com.QhomeBase.servicescardservice.jobs;

import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.ElevatorCardRegistrationRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job để tự động cập nhật trạng thái thẻ dựa trên thời gian:
 * - Sau 30 ngày từ lúc admin approve: Chuyển sang "NEEDS_RENEWAL" (cần gia hạn)
 * - Sau 36 ngày từ lúc admin approve: Chuyển sang "SUSPENDED" (tạm ngưng)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CardStatusUpdateScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_NEEDS_RENEWAL = "NEEDS_RENEWAL";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String PAYMENT_STATUS_PAID = "PAID";

    private final ResidentCardRegistrationRepository residentCardRepository;
    private final ElevatorCardRegistrationRepository elevatorCardRepository;
    private final RegisterServiceRequestRepository vehicleCardRepository;

    @Value("${card.fee.cycle-months:30}")
    private int cycleMonths;

    @Value("${card.fee.cycle-days:900}")
    private int cycleDays;

    @Value("${card.status.update.needs-renewal-months:30}")
    private int needsRenewalMonths;

    @Value("${card.status.update.suspend-after-days:6}")
    private int suspendAfterDays;

    @Value("${card.status.update.enabled:true}")
    private boolean statusUpdateEnabled;

    /**
     * Scheduled job chạy mỗi ngày lúc 08:00 để cập nhật trạng thái thẻ.
     */
    @Scheduled(cron = "${card.status.update.cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void updateCardStatuses() {
        if (!statusUpdateEnabled) {
            log.debug("ℹ️ [CardStatusUpdate] Status update is disabled via configuration");
            return;
        }

        try {
            int updatedNeedsRenewal = 0;
            int updatedSuspended = 0;

            // Update Resident Cards
            List<ResidentCardRegistration> residentCards = residentCardRepository
                    .findByStatusAndPaymentStatus(STATUS_APPROVED, PAYMENT_STATUS_PAID);
            for (ResidentCardRegistration card : residentCards) {
                if (card.getApprovedAt() == null) continue;

                // Production mode: check months and days
                LocalDate approvedDate = card.getApprovedAt().atZoneSameInstant(ZONE).toLocalDate();
                LocalDate today = LocalDate.now(ZONE);
                long monthsSinceApproval = ChronoUnit.MONTHS.between(approvedDate, today);
                long daysSinceApproval = ChronoUnit.DAYS.between(approvedDate, today);
                
                // Sau 30 tháng từ lúc approve: NEEDS_RENEWAL
                // Sau 30 tháng + 6 ngày từ lúc approve: SUSPENDED
                long needsRenewalThresholdMonths = needsRenewalMonths; // 30 tháng
                long suspendedThresholdDays = (needsRenewalMonths * 30L) + suspendAfterDays; // 30 tháng + 6 ngày

                if (daysSinceApproval >= suspendedThresholdDays) {
                    // SUSPENDED: Sau 30 tháng + 6 ngày
                    if (!STATUS_SUSPENDED.equals(card.getStatus())) {
                        card.setStatus(STATUS_SUSPENDED);
                        updatedSuspended++;
                        log.info("🔄 [CardStatusUpdate] Resident card {} chuyển sang SUSPENDED ({} tháng {} ngày từ khi approve)",
                                card.getId(), monthsSinceApproval, daysSinceApproval % 30);
                    }
                } else if (monthsSinceApproval >= needsRenewalThresholdMonths) {
                    // NEEDS_RENEWAL: Sau 30 tháng
                    if (!STATUS_NEEDS_RENEWAL.equals(card.getStatus())) {
                        card.setStatus(STATUS_NEEDS_RENEWAL);
                        updatedNeedsRenewal++;
                        log.info("🔄 [CardStatusUpdate] Resident card {} chuyển sang NEEDS_RENEWAL ({} tháng từ khi approve)",
                                card.getId(), monthsSinceApproval);
                    }
                }
            }

            // Update Elevator Cards
            List<ElevatorCardRegistration> elevatorCards = elevatorCardRepository
                    .findByStatusAndPaymentStatus(STATUS_APPROVED, PAYMENT_STATUS_PAID);
            for (ElevatorCardRegistration card : elevatorCards) {
                if (card.getApprovedAt() == null) continue;

                // Production mode: check months and days
                LocalDate approvedDate = card.getApprovedAt().atZoneSameInstant(ZONE).toLocalDate();
                LocalDate today = LocalDate.now(ZONE);
                long monthsSinceApproval = ChronoUnit.MONTHS.between(approvedDate, today);
                long daysSinceApproval = ChronoUnit.DAYS.between(approvedDate, today);
                
                // Sau 30 tháng từ lúc approve: NEEDS_RENEWAL
                // Sau 30 tháng + 6 ngày từ lúc approve: SUSPENDED
                long needsRenewalThresholdMonths = needsRenewalMonths; // 30 tháng
                long suspendedThresholdDays = (needsRenewalMonths * 30L) + suspendAfterDays; // 30 tháng + 6 ngày

                if (daysSinceApproval >= suspendedThresholdDays) {
                    // SUSPENDED: Sau 30 tháng + 6 ngày
                    if (!STATUS_SUSPENDED.equals(card.getStatus())) {
                        card.setStatus(STATUS_SUSPENDED);
                        updatedSuspended++;
                        log.info("🔄 [CardStatusUpdate] Elevator card {} chuyển sang SUSPENDED ({} tháng {} ngày từ khi approve)",
                                card.getId(), monthsSinceApproval, daysSinceApproval % 30);
                    }
                } else if (monthsSinceApproval >= needsRenewalThresholdMonths) {
                    // NEEDS_RENEWAL: Sau 30 tháng
                    if (!STATUS_NEEDS_RENEWAL.equals(card.getStatus())) {
                        card.setStatus(STATUS_NEEDS_RENEWAL);
                        updatedNeedsRenewal++;
                        log.info("🔄 [CardStatusUpdate] Elevator card {} chuyển sang NEEDS_RENEWAL ({} tháng từ khi approve)",
                                card.getId(), monthsSinceApproval);
                    }
                }
            }

            // Update Vehicle Cards
            List<RegisterServiceRequest> vehicleCards = vehicleCardRepository
                    .findByStatusAndPaymentStatus(STATUS_APPROVED, PAYMENT_STATUS_PAID);
            for (RegisterServiceRequest card : vehicleCards) {
                if (card.getApprovedAt() == null) continue;

                // Production mode: check months and days
                LocalDate approvedDate = card.getApprovedAt().atZoneSameInstant(ZONE).toLocalDate();
                LocalDate today = LocalDate.now(ZONE);
                long monthsSinceApproval = ChronoUnit.MONTHS.between(approvedDate, today);
                long daysSinceApproval = ChronoUnit.DAYS.between(approvedDate, today);
                
                // Sau 30 tháng từ lúc approve: NEEDS_RENEWAL
                // Sau 30 tháng + 6 ngày từ lúc approve: SUSPENDED
                long needsRenewalThresholdMonths = needsRenewalMonths; // 30 tháng
                long suspendedThresholdDays = (needsRenewalMonths * 30L) + suspendAfterDays; // 30 tháng + 6 ngày

                if (daysSinceApproval >= suspendedThresholdDays) {
                    // SUSPENDED: Sau 30 tháng + 6 ngày
                    if (!STATUS_SUSPENDED.equals(card.getStatus())) {
                        card.setStatus(STATUS_SUSPENDED);
                        updatedSuspended++;
                        log.info("🔄 [CardStatusUpdate] Vehicle card {} chuyển sang SUSPENDED ({} tháng {} ngày từ khi approve)",
                                card.getId(), monthsSinceApproval, daysSinceApproval % 30);
                    }
                } else if (monthsSinceApproval >= needsRenewalThresholdMonths) {
                    // NEEDS_RENEWAL: Sau 30 tháng
                    if (!STATUS_NEEDS_RENEWAL.equals(card.getStatus())) {
                        card.setStatus(STATUS_NEEDS_RENEWAL);
                        updatedNeedsRenewal++;
                        log.info("🔄 [CardStatusUpdate] Vehicle card {} chuyển sang NEEDS_RENEWAL ({} tháng từ khi approve)",
                                card.getId(), monthsSinceApproval);
                    }
                }
            }

            if (updatedNeedsRenewal > 0 || updatedSuspended > 0) {
                log.info("✅ [CardStatusUpdate] Đã cập nhật {} thẻ sang NEEDS_RENEWAL, {} thẻ sang SUSPENDED",
                        updatedNeedsRenewal, updatedSuspended);
            } else {
                log.debug("ℹ️ [CardStatusUpdate] Không có thẻ nào cần cập nhật trạng thái");
            }
        } catch (Exception ex) {
            log.error("❌ [CardStatusUpdate] Lỗi khi cập nhật trạng thái thẻ", ex);
        }
    }
}

