package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.model.CardFeeReminderState;
import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.CardFeeReminderStateRepository;
import com.QhomeBase.servicescardservice.repository.ElevatorCardRegistrationRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardFeeReminderService {

    public enum CardFeeType {
        RESIDENT, ELEVATOR, VEHICLE
    }

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CardFeeReminderStateRepository reminderStateRepository;
    private final ResidentCardRegistrationRepository residentCardRepository;
    private final ElevatorCardRegistrationRepository elevatorCardRepository;
    private final RegisterServiceRequestRepository vehicleRegistrationRepository;
    private final ResidentUnitLookupService residentUnitLookupService;

    private final Set<UUID> vehicleCardsMissingResident = ConcurrentHashMap.newKeySet();

    @Value("${card.fee.cycle-months:30}")
    private int cycleMonths;

    @Value("${card.fee.cycle-days:900}")
    private int cycleDays;

    @Value("${card.fee.reminder.interval-hours:24}")
    private int reminderIntervalHours;

    @Value("${card.fee.reminder.grace-days:6}")
    private int graceDays;

    @Value("${card.fee.reminder.max-per-cycle:6}")
    private int maxRemindersPerCycle;

    /**
     * Reset reminder state after successful payment.
     * This starts a new payment cycle.
     * Note: Cycle tính từ approved_at (ngày admin duyệt), không phải payment_date.
     */
    @Transactional
    public void resetReminderAfterPayment(CardFeeType cardType, UUID cardId,
                                          UUID unitId, UUID residentId, UUID userId,
                                          String apartmentNumber, String buildingName,
                                          OffsetDateTime paymentDate) {
        if (cardId == null) {
            log.warn("⚠️ [CardFeeReminder] Bỏ qua reset vì thiếu cardId");
            return;
        }

        try {
            // Lấy approved_at từ card entity để tính cycle (ưu tiên approved_at)
            OffsetDateTime approvedAt = getApprovedAtFromCard(cardType, cardId);
            LocalDate cycleStart = resolveCycleStart(paymentDate, approvedAt, null);
            // Tính nextDueDate: cycleStart + 30 tháng (900 ngày)
            LocalDate nextDue = cycleStart.plusMonths(getSafeCycleMonths());

            CardFeeReminderState state = reminderStateRepository
                    .findByCardTypeAndCardId(cardType.name(), cardId)
                    .orElseGet(() -> CardFeeReminderState.builder()
                            .cardType(cardType.name())
                            .cardId(cardId)
                            .maxReminders(Math.max(1, maxRemindersPerCycle))
                            .build());

            state.setUnitId(unitId);
            state.setResidentId(residentId);
            state.setUserId(userId);
            state.setApartmentNumber(truncate(apartmentNumber));
            state.setBuildingName(truncate(buildingName));
            state.setCycleStartDate(cycleStart);
            state.setNextDueDate(nextDue);
            state.setReminderCount(0);
            state.setLastRemindedAt(null);
            state.setMaxReminders(Math.max(1, maxRemindersPerCycle));

            reminderStateRepository.save(state);
            log.debug("✅ [CardFeeReminder] Reset chu kỳ nhắc phí {} cho card {} (next due: {})", 
                    cardType, cardId, nextDue);
        } catch (Exception e) {
            log.error("❌ [CardFeeReminder] Lỗi reset reminder state cho {} card {}: {}", 
                    cardType, cardId, e.getMessage(), e);
        }
    }

    /**
     * Sync all active paid cards into reminder state table.
     * This ensures all cards that need reminders are tracked.
     */
    @Transactional
    public void syncActiveCardsIntoReminderState() {
        try {
            syncResidentCardStates();
            syncElevatorCardStates();
            syncVehicleCardStates();
            log.debug("✅ [CardFeeReminder] Đã đồng bộ reminder state cho tất cả active cards");
        } catch (Exception e) {
            log.error("❌ [CardFeeReminder] Lỗi sync active cards vào reminder state: {}", e.getMessage(), e);
        }
    }

    /**
     * Find all reminder states that are due for notification today.
     */
    @Transactional(readOnly = true)
    public List<CardFeeReminderState> findDueStates(LocalDate today) {
        LocalDate safeToday = today != null ? today : LocalDate.now(DEFAULT_ZONE);
        LocalDate cutoffDate = safeToday.minusDays(getSafeGraceDays());

        List<CardFeeReminderState> states = reminderStateRepository.findDueStates(safeToday, cutoffDate);

        // Filter out states that were already reminded
        // Production mode: check hours (send every 24 hours)
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);
        
        return states.stream()
                .filter(state -> {
                    // First, check if card is still active (not cancelled, suspended, or rejected)
                    // Nếu thẻ bị hủy (CANCELLED) trong khoảng 6 ngày thì không gửi notification nữa
                    if (!isCardActive(state)) {
                        return false; // Skip reminder for inactive cards
                    }
                    
                    // Kiểm tra nếu đã gửi đủ số lần reminder tối đa (6 lần)
                    if (state.getReminderCount() >= maxRemindersPerCycle) {
                        return false; // Đã gửi đủ 6 lần, không gửi nữa
                    }
                    
                    // Production mode: check hours - send every 24 hours
                    if (state.getLastRemindedAt() == null) {
                        // First reminder: check if nextDueDate has passed
                        return state.getNextDueDate() != null && 
                               !state.getNextDueDate().isAfter(safeToday);
                    } else {
                        // Subsequent reminders: check if at least 24 hours have passed since last reminder
                        long hoursSinceLastReminder = java.time.Duration.between(
                                state.getLastRemindedAt(), now).toHours();
                        return hoursSinceLastReminder >= reminderIntervalHours;
                    }
                })
                .toList();
    }

    /**
     * Mark reminder as sent for given states.
     */
    @Transactional
    public void markReminderSent(List<CardFeeReminderState> states) {
        if (states == null || states.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_ZONE);
        states.forEach(state -> {
            state.setReminderCount(state.getReminderCount() + 1);
            state.setLastRemindedAt(now);
        });
        reminderStateRepository.saveAll(states);
    }

    /**
     * Calculate days since due date.
     */
    public long daysSinceDue(CardFeeReminderState state, LocalDate today) {
        if (state == null || state.getNextDueDate() == null) {
            return 0;
        }
        LocalDate base = today != null ? today : LocalDate.now(DEFAULT_ZONE);
        return Math.max(0, ChronoUnit.DAYS.between(state.getNextDueDate(), base));
    }

    /**
     * Update recipient info if missing (for cases where resident info is resolved later).
     */
    @Transactional
    public void updateRecipientInfo(CardFeeReminderState state, UUID residentId,
                                     String apartmentNumber, String buildingName) {
        if (state == null) {
            return;
        }
        boolean changed = false;
        if (residentId != null && state.getResidentId() == null) {
            state.setResidentId(residentId);
            changed = true;
        }
        if (StringUtils.hasText(apartmentNumber) && !StringUtils.hasText(state.getApartmentNumber())) {
            state.setApartmentNumber(truncate(apartmentNumber));
            changed = true;
        }
        if (StringUtils.hasText(buildingName) && !StringUtils.hasText(state.getBuildingName())) {
            state.setBuildingName(truncate(buildingName));
            changed = true;
        }
        if (changed) {
            reminderStateRepository.save(state);
        }
    }

    // Private helper methods

    private void syncResidentCardStates() {
        List<ResidentCardRegistration> cards = residentCardRepository.findActivePaidCards();
        for (ResidentCardRegistration card : cards) {
            ensureStateForResidentCard(card);
        }
    }

    private void syncElevatorCardStates() {
        List<ElevatorCardRegistration> cards = elevatorCardRepository.findActivePaidCards();
        for (ElevatorCardRegistration card : cards) {
            ensureStateForElevatorCard(card);
        }
    }

    private void syncVehicleCardStates() {
        List<RegisterServiceRequest> cards = vehicleRegistrationRepository.findActivePaidCards("VEHICLE_REGISTRATION");
        for (RegisterServiceRequest card : cards) {
            ensureStateForVehicleCard(card);
        }
    }

    private void ensureStateForResidentCard(ResidentCardRegistration card) {
        if (card == null) {
            return;
        }
        Optional<CardFeeReminderState> existing = reminderStateRepository
                .findByCardTypeAndCardId(CardFeeType.RESIDENT.name(), card.getId());
        if (existing.isPresent()) {
            updateAddressIfMissing(existing.get(), card.getApartmentNumber(), card.getBuildingName(),
                    card.getResidentId(), card.getUserId(), card.getUnitId());
            return;
        }

        LocalDate cycleStart = resolveCycleStart(card.getPaymentDate(), card.getApprovedAt(), card.getCreatedAt());
        CardFeeReminderState state = CardFeeReminderState.builder()
                .cardType(CardFeeType.RESIDENT.name())
                .cardId(card.getId())
                .unitId(card.getUnitId())
                .residentId(card.getResidentId())
                .userId(card.getUserId())
                .apartmentNumber(truncate(card.getApartmentNumber()))
                .buildingName(truncate(card.getBuildingName()))
                .cycleStartDate(cycleStart)
                .nextDueDate(cycleStart.plusMonths(getSafeCycleMonths()))
                .reminderCount(0)
                .maxReminders(Math.max(1, maxRemindersPerCycle))
                .build();
        reminderStateRepository.save(state);
    }

    private void ensureStateForElevatorCard(ElevatorCardRegistration card) {
        if (card == null) {
            return;
        }
        Optional<CardFeeReminderState> existing = reminderStateRepository
                .findByCardTypeAndCardId(CardFeeType.ELEVATOR.name(), card.getId());
        if (existing.isPresent()) {
            updateAddressIfMissing(existing.get(), card.getApartmentNumber(), card.getBuildingName(),
                    card.getResidentId(), card.getUserId(), card.getUnitId());
            return;
        }

        LocalDate cycleStart = resolveCycleStart(card.getPaymentDate(), card.getApprovedAt(), card.getCreatedAt());
        CardFeeReminderState state = CardFeeReminderState.builder()
                .cardType(CardFeeType.ELEVATOR.name())
                .cardId(card.getId())
                .unitId(card.getUnitId())
                .residentId(card.getResidentId())
                .userId(card.getUserId())
                .apartmentNumber(truncate(card.getApartmentNumber()))
                .buildingName(truncate(card.getBuildingName()))
                .cycleStartDate(cycleStart)
                .nextDueDate(cycleStart.plusMonths(getSafeCycleMonths()))
                .reminderCount(0)
                .maxReminders(Math.max(1, maxRemindersPerCycle))
                .build();
        reminderStateRepository.save(state);
    }

    private void ensureStateForVehicleCard(RegisterServiceRequest card) {
        if (card == null) {
            return;
        }
        Optional<CardFeeReminderState> existing = reminderStateRepository
                .findByCardTypeAndCardId(CardFeeType.VEHICLE.name(), card.getId());
        if (existing.isPresent()) {
            UUID residentId = residentUnitLookupService.resolveByUser(card.getUserId(), card.getUnitId())
                    .map(ResidentUnitLookupService.AddressInfo::residentId)
                    .orElse(null);
            updateAddressIfMissing(existing.get(), card.getApartmentNumber(), card.getBuildingName(),
                    residentId, card.getUserId(), card.getUnitId());
            return;
        }

        UUID residentId = residentUnitLookupService.resolveByUser(card.getUserId(), card.getUnitId())
                .map(ResidentUnitLookupService.AddressInfo::residentId)
                .orElse(null);
        if (residentId == null) {
            if (vehicleCardsMissingResident.add(card.getId())) {
                log.warn("⚠️ [CardFeeReminder] Không tìm thấy residentId cho đăng ký thẻ xe {}, bỏ qua nhắc phí", card.getId());
            }
            return;
        }
        vehicleCardsMissingResident.remove(card.getId());

        LocalDate cycleStart = resolveCycleStart(card.getPaymentDate(), card.getApprovedAt(), card.getCreatedAt());
        CardFeeReminderState state = CardFeeReminderState.builder()
                .cardType(CardFeeType.VEHICLE.name())
                .cardId(card.getId())
                .unitId(card.getUnitId())
                .residentId(residentId)
                .userId(card.getUserId())
                .apartmentNumber(truncate(card.getApartmentNumber()))
                .buildingName(truncate(card.getBuildingName()))
                .cycleStartDate(cycleStart)
                .nextDueDate(cycleStart.plusMonths(getSafeCycleMonths()))
                .reminderCount(0)
                .maxReminders(Math.max(1, maxRemindersPerCycle))
                .build();
        reminderStateRepository.save(state);
    }

    private void updateAddressIfMissing(CardFeeReminderState state, String apartmentNumber,
                                         String buildingName, UUID residentId, UUID userId, UUID unitId) {
        boolean changed = false;
        if (state.getResidentId() == null && residentId != null) {
            state.setResidentId(residentId);
            changed = true;
        }
        if (state.getUserId() == null && userId != null) {
            state.setUserId(userId);
            changed = true;
        }
        if (state.getUnitId() == null && unitId != null) {
            state.setUnitId(unitId);
            changed = true;
        }
        if (!StringUtils.hasText(state.getApartmentNumber()) && StringUtils.hasText(apartmentNumber)) {
            state.setApartmentNumber(truncate(apartmentNumber));
            changed = true;
        }
        if (!StringUtils.hasText(state.getBuildingName()) && StringUtils.hasText(buildingName)) {
            state.setBuildingName(truncate(buildingName));
            changed = true;
        }
        if (state.getCycleStartDate() == null) {
            state.setCycleStartDate(LocalDate.now(DEFAULT_ZONE));
            state.setNextDueDate(state.getCycleStartDate().plusMonths(getSafeCycleMonths()));
            changed = true;
        }
        if (state.getMaxReminders() <= 0) {
            state.setMaxReminders(Math.max(1, maxRemindersPerCycle));
            changed = true;
        }
        if (changed) {
            reminderStateRepository.save(state);
        }
    }

    /**
     * Resolve cycle start date for reminder calculation.
     * Priority: approved_at > payment_date > created_at
     * 
     * Logic: Tính từ ngày admin duyệt thẻ (approved_at) vì thẻ chỉ có hiệu lực sau khi được duyệt.
     * Nếu chưa có approved_at thì fallback sang payment_date hoặc created_at.
     */
    private LocalDate resolveCycleStart(OffsetDateTime paymentDate, OffsetDateTime approvedAt, OffsetDateTime createdAt) {
        // Ưu tiên approved_at vì thẻ chỉ có hiệu lực sau khi admin duyệt
        if (approvedAt != null) {
            return approvedAt.atZoneSameInstant(DEFAULT_ZONE).toLocalDate();
        }
        // Fallback sang payment_date nếu chưa có approved_at
        if (paymentDate != null) {
            return paymentDate.atZoneSameInstant(DEFAULT_ZONE).toLocalDate();
        }
        // Cuối cùng dùng created_at nếu không có cả 2
        if (createdAt != null) {
            return createdAt.atZoneSameInstant(DEFAULT_ZONE).toLocalDate();
        }
        return LocalDate.now(DEFAULT_ZONE);
    }

    private int getSafeCycleMonths() {
        // Use cycleMonths (30 months) for production
        return Math.max(1, cycleMonths);
    }

    private int getSafeCycleDays() {
        // Fallback to cycleDays if needed
        return Math.max(0, cycleDays);
    }

    private int getSafeGraceDays() {
        return Math.max(0, graceDays);
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    /**
     * Check if card is still active (not cancelled, suspended, or rejected).
     * Cards that are cancelled, suspended, or rejected should not receive reminders.
     * Đặc biệt: Nếu thẻ bị hủy (CANCELLED) trong khoảng 6 ngày reminder thì không gửi notification nữa.
     */
    private boolean isCardActive(CardFeeReminderState state) {
        try {
            CardFeeType cardType = CardFeeType.valueOf(state.getCardType());
            UUID cardId = state.getCardId();
            
            boolean isActive = switch (cardType) {
                case RESIDENT -> residentCardRepository.findById(cardId)
                        .map(card -> {
                            String status = card.getStatus();
                            // Exclude cancelled, suspended, and rejected cards
                            return status != null 
                                && !status.equalsIgnoreCase("CANCELLED")
                                && !status.equalsIgnoreCase("SUSPENDED")
                                && !status.equalsIgnoreCase("REJECTED");
                        })
                        .orElse(false); // If card not found, consider inactive
                case ELEVATOR -> elevatorCardRepository.findById(cardId)
                        .map(card -> {
                            String status = card.getStatus();
                            // Exclude cancelled, suspended, and rejected cards
                            return status != null 
                                && !status.equalsIgnoreCase("CANCELLED")
                                && !status.equalsIgnoreCase("SUSPENDED")
                                && !status.equalsIgnoreCase("REJECTED");
                        })
                        .orElse(false); // If card not found, consider inactive
                case VEHICLE -> vehicleRegistrationRepository.findById(cardId)
                        .map(card -> {
                            String status = card.getStatus();
                            // Exclude cancelled, suspended, and rejected cards
                            return status != null 
                                && !status.equalsIgnoreCase("CANCELLED")
                                && !status.equalsIgnoreCase("SUSPENDED")
                                && !status.equalsIgnoreCase("REJECTED");
                        })
                        .orElse(false); // If card not found, consider inactive
                default -> false;
            };
            
            // Nếu thẻ bị hủy (CANCELLED) và đang trong khoảng 6 ngày reminder thì không gửi notification
            if (!isActive && state.getReminderCount() > 0 && state.getReminderCount() < maxRemindersPerCycle) {
                log.debug("⚠️ [CardFeeReminder] Thẻ {} {} đã bị hủy trong khoảng reminder, không gửi notification nữa", 
                        state.getCardType(), state.getCardId());
            }
            
            return isActive;
        } catch (Exception e) {
            log.warn("⚠️ [CardFeeReminder] Không thể kiểm tra trạng thái card {} {}: {}", 
                    state.getCardType(), state.getCardId(), e.getMessage());
            return false; // On error, consider inactive to be safe
        }
    }

    /**
     * Get approved_at date from card entity.
     * Used when resetting reminder state to calculate cycle from approval date.
     */
    private OffsetDateTime getApprovedAtFromCard(CardFeeType cardType, UUID cardId) {
        try {
            switch (cardType) {
                case RESIDENT -> {
                    return residentCardRepository.findById(cardId)
                            .map(ResidentCardRegistration::getApprovedAt)
                            .orElse(null);
                }
                case ELEVATOR -> {
                    return elevatorCardRepository.findById(cardId)
                            .map(ElevatorCardRegistration::getApprovedAt)
                            .orElse(null);
                }
                case VEHICLE -> {
                    return vehicleRegistrationRepository.findById(cardId)
                            .map(RegisterServiceRequest::getApprovedAt)
                            .orElse(null);
                }
                default -> {
                    return null;
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ [CardFeeReminder] Không thể lấy approved_at cho {} card {}: {}", 
                    cardType, cardId, e.getMessage());
            return null;
        }
    }
}
