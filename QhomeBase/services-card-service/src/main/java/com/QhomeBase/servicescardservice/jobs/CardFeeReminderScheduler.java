package com.QhomeBase.servicescardservice.jobs;

import com.QhomeBase.servicescardservice.model.CardFeeReminderState;
import com.QhomeBase.servicescardservice.service.CardFeeReminderService;
import com.QhomeBase.servicescardservice.service.CardFeeReminderService.CardFeeType;
import com.QhomeBase.servicescardservice.service.NotificationClient;
import com.QhomeBase.servicescardservice.service.ResidentUnitLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardFeeReminderScheduler {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CardFeeReminderService reminderService;
    private final NotificationClient notificationClient;
    private final ResidentUnitLookupService residentUnitLookupService;

    @Value("${card.fee.reminder.enabled:true}")
    private boolean remindersEnabled;

    @Value("${card.fee.reminder.grace-days:6}")
    private int graceDays;

    @Value("${card.fee.cycle-months:30}")
    private int cycleMonths;

    @Value("${card.fee.cycle-days:900}")
    private int cycleDays;

    /**
     * Scheduled job chạy mỗi ngày lúc 08:00 để gửi reminder đóng phí thẻ.
     * Logic:
     * 1. Sync active cards vào reminder state (đảm bảo tất cả cards đã thanh toán đều được track)
     * 2. Tìm các reminder states đã đến hạn (next_due_date <= today và <= cutoffDate)
     * 3. Gom theo unit và gửi realtime notification + FCM push notification
     * 4. Mark reminder đã gửi để tránh duplicate
     */
    @Scheduled(cron = "${card.fee.reminder.cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void executeReminderJob() {
        if (!remindersEnabled) {
            log.debug("ℹ️ [CardFeeReminderJob] Reminders are disabled via configuration");
            return;
        }

        try {
            // Sync active cards vào reminder state (đảm bảo tracking đầy đủ)
            reminderService.syncActiveCardsIntoReminderState();

            LocalDate today = LocalDate.now(ZONE);
            List<CardFeeReminderState> dueStates = reminderService.findDueStates(today);
            
            if (CollectionUtils.isEmpty(dueStates)) {
                log.debug("ℹ️ [CardFeeReminderJob] No card fees due on {}", today);
                return;
            }

            // Gom theo unit và gửi notification
            List<ReminderBatch> batches = buildBatches(dueStates, today);
            if (batches.isEmpty()) {
                log.debug("ℹ️ [CardFeeReminderJob] No batches ready after filtering recipient data");
                return;
            }

            int notificationCount = 0;
            List<CardFeeReminderState> processedStates = new ArrayList<>();

            for (ReminderBatch batch : batches) {
                // IMPORTANT: Gửi notification riêng cho từng resident
                // Mỗi resident sẽ nhận notification riêng tư về thẻ của họ
                for (UUID residentId : batch.residentIds) {
                    if (residentId == null) {
                        continue;
                    }
                    // Lọc states chỉ cho resident này
                    List<CardFeeReminderState> residentStates = batch.states.stream()
                            .filter(state -> {
                                UUID stateResidentId = ensureResident(state);
                                return stateResidentId != null && stateResidentId.equals(residentId);
                            })
                            .toList();
                    
                    if (residentStates.isEmpty()) {
                        continue;
                    }
                    
                    // Tạo batch riêng cho resident này với chỉ thẻ của họ
                    ReminderBatch residentBatch = createResidentBatch(batch, residentId, residentStates);
                    
                    // Gửi realtime notification + FCM push notification (riêng tư)
                    sendReminder(residentBatch, residentId);
                    notificationCount++;
                }
                processedStates.addAll(batch.states);
            }

            // Mark reminder đã gửi để tránh duplicate trong cùng ngày
            if (!processedStates.isEmpty()) {
                reminderService.markReminderSent(processedStates);
            }

            log.info("✅ [CardFeeReminderJob] Đã gửi {} thông báo cho {} nhóm thẻ ({} bản ghi)",
                    notificationCount, batches.size(), processedStates.size());
        } catch (Exception ex) {
            log.error("❌ [CardFeeReminderJob] Lỗi khi chạy job nhắc đóng phí thẻ", ex);
        }
    }

    private List<ReminderBatch> buildBatches(List<CardFeeReminderState> states, LocalDate today) {
        Map<String, ReminderBatch> batches = new LinkedHashMap<>();

        for (CardFeeReminderState state : states) {
            UUID residentId = ensureResident(state);
            if (residentId == null) {
                log.warn("⚠️ [CardFeeReminderJob] Bỏ qua card {} vì không tìm thấy residentId", state.getCardId());
                continue;
            }

            CardFeeType cardType = parseType(state.getCardType());
            if (cardType == null) {
                log.warn("⚠️ [CardFeeReminderJob] Card {} có cardType không hợp lệ: {}", 
                        state.getCardId(), state.getCardType());
                continue;
            }

            String key = Optional.ofNullable(state.getUnitId()).map(UUID::toString).orElse("NO_UNIT");
            ReminderBatch batch = batches.computeIfAbsent(key, k -> new ReminderBatch(
                    state.getUnitId(),
                    new LinkedHashSet<>(),
                    new EnumMap<>(CardFeeType.class),
                    new ArrayList<>(),
                    state.getApartmentNumber(),
                    state.getBuildingName()
            ));

            batch.residentIds.add(residentId);
            batch.counts.put(cardType, batch.counts.getOrDefault(cardType, 0) + 1);
            batch.states.add(state);
            batch.maxDaysSinceDue = Math.max(batch.maxDaysSinceDue, 
                    reminderService.daysSinceDue(state, today));
        }

        return new ArrayList<>(batches.values());
    }

    private ReminderBatch createResidentBatch(ReminderBatch originalBatch, UUID residentId, List<CardFeeReminderState> residentStates) {
        Map<CardFeeType, Integer> residentCounts = new EnumMap<>(CardFeeType.class);
        long maxDaysSinceDue = 0;
        LocalDate today = LocalDate.now(ZONE);
        
        for (CardFeeReminderState state : residentStates) {
            CardFeeType cardType = parseType(state.getCardType());
            if (cardType != null) {
                residentCounts.put(cardType, residentCounts.getOrDefault(cardType, 0) + 1);
            }
            maxDaysSinceDue = Math.max(maxDaysSinceDue, reminderService.daysSinceDue(state, today));
        }
        
        ReminderBatch residentBatch = new ReminderBatch(
                originalBatch.unitId,
                Set.of(residentId),
                residentCounts,
                residentStates,
                originalBatch.apartmentNumber,
                originalBatch.buildingName
        );
        residentBatch.maxDaysSinceDue = maxDaysSinceDue;
        return residentBatch;
    }

    private void sendReminder(ReminderBatch batch, UUID residentId) {
        String unitLabel = buildUnitLabel(batch.apartmentNumber, batch.buildingName);
        String countsText = buildCountsText(batch.counts);
        long remainingDays = Math.max(0, graceDays - batch.maxDaysSinceDue);

        String title = "Nhắc đóng phí thẻ dịch vụ";
        String message = String.format(
                "%s đang có %s đến hạn thanh toán sau %d tháng sử dụng. Vui lòng hoàn tất trong %d ngày tới.",
                unitLabel,
                countsText,
                cycleMonths,
                remainingDays);

        Map<String, String> data = new HashMap<>();
        data.put("unitId", Optional.ofNullable(batch.unitId).map(UUID::toString).orElse(""));
        data.put("apartmentNumber", batch.apartmentNumber != null ? batch.apartmentNumber : "");
        data.put("buildingName", batch.buildingName != null ? batch.buildingName : "");
        data.put("vehicleCardsDue", String.valueOf(batch.counts.getOrDefault(CardFeeType.VEHICLE, 0)));
        data.put("elevatorCardsDue", String.valueOf(batch.counts.getOrDefault(CardFeeType.ELEVATOR, 0)));
        data.put("residentCardsDue", String.valueOf(batch.counts.getOrDefault(CardFeeType.RESIDENT, 0)));
        data.put("reminderType", "CARD_FEE");

        // Gửi realtime notification (WebSocket) + FCM push notification
        // NotificationClient sẽ tự động gửi cả 2 loại:
        // 1. Realtime notification qua WebSocket cho app đang mở
        // 2. FCM push notification cho app đang đóng
        notificationClient.sendResidentNotification(
                residentId,
                null, // buildingId - sẽ được resolve từ unitId nếu cần
                "CARD_FEE_REMINDER",
                title,
                message,
                null, // referenceId - không cần vì đây là reminder tổng hợp
                "CARD_FEE",
                data
        );

        log.info("🔔 [CardFeeReminderJob] Sent reminder to resident {} for unit {} ({})",
                residentId,
                batch.unitId,
                countsText);
    }

    private UUID ensureResident(CardFeeReminderState state) {
        if (state.getResidentId() != null) {
            return state.getResidentId();
        }
        if (state.getUserId() == null) {
            return null;
        }
        return residentUnitLookupService.resolveByUser(state.getUserId(), state.getUnitId())
                .map(info -> {
                    // Update reminder state với resident info nếu thiếu
                    reminderService.updateRecipientInfo(
                            state,
                            info.residentId(),
                            info.apartmentNumber(),
                            info.buildingName());
                    return info.residentId();
                })
                .orElse(null);
    }

    private CardFeeType parseType(String value) {
        if (!org.springframework.util.StringUtils.hasText(value)) {
            return null;
        }
        try {
            return CardFeeType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildUnitLabel(String apartmentNumber, String buildingName) {
        if (org.springframework.util.StringUtils.hasText(apartmentNumber) 
                && org.springframework.util.StringUtils.hasText(buildingName)) {
            return String.format("Căn %s - %s", apartmentNumber, buildingName);
        }
        if (org.springframework.util.StringUtils.hasText(apartmentNumber)) {
            return "Căn " + apartmentNumber;
        }
        if (org.springframework.util.StringUtils.hasText(buildingName)) {
            return buildingName;
        }
        return "Căn hộ";
    }

    private String buildCountsText(Map<CardFeeType, Integer> counts) {
        List<String> parts = new ArrayList<>();
        int vehicleCount = counts.getOrDefault(CardFeeType.VEHICLE, 0);
        int elevatorCount = counts.getOrDefault(CardFeeType.ELEVATOR, 0);
        int residentCount = counts.getOrDefault(CardFeeType.RESIDENT, 0);

        if (vehicleCount > 0) {
            parts.add(vehicleCount + " thẻ xe");
        }
        if (elevatorCount > 0) {
            parts.add(elevatorCount + " thẻ thang máy");
        }
        if (residentCount > 0) {
            parts.add(residentCount + " thẻ cư dân");
        }

        if (parts.isEmpty()) {
            return "thẻ dịch vụ";
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        if (parts.size() == 2) {
            return parts.get(0) + " và " + parts.get(1);
        }
        return String.join(", ", parts.subList(0, parts.size() - 1)) 
                + " và " + parts.get(parts.size() - 1);
    }

    private static class ReminderBatch {
        final UUID unitId;
        final Set<UUID> residentIds;
        final Map<CardFeeType, Integer> counts;
        final List<CardFeeReminderState> states;
        final String apartmentNumber;
        final String buildingName;
        long maxDaysSinceDue;

        ReminderBatch(UUID unitId, Set<UUID> residentIds, Map<CardFeeType, Integer> counts,
                      List<CardFeeReminderState> states, String apartmentNumber, String buildingName) {
            this.unitId = unitId;
            this.residentIds = residentIds;
            this.counts = counts;
            this.states = states;
            this.apartmentNumber = apartmentNumber;
            this.buildingName = buildingName;
            this.maxDaysSinceDue = 0;
        }
    }
}
