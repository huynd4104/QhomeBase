package com.QhomeBase.servicescardservice.controller;

import com.QhomeBase.servicescardservice.jobs.CardFeeReminderScheduler;
import com.QhomeBase.servicescardservice.model.CardFeeReminderState;
import com.QhomeBase.servicescardservice.repository.CardFeeReminderStateRepository;
import com.QhomeBase.servicescardservice.service.CardFeeReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Test controller for card fee reminder functionality.
 * Use this controller to test reminder logic without waiting 30 days.
 * 
 * WARNING: This is for testing only. Remove or secure this controller in production.
 */
@RestController
@RequestMapping("/api/test/card-fee-reminder")
@RequiredArgsConstructor
@Slf4j
public class CardFeeReminderTestController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CardFeeReminderStateRepository reminderStateRepository;
    private final CardFeeReminderScheduler reminderScheduler;
    private final CardFeeReminderService reminderService;

    /**
     * Set next_due_date to 5 minutes from now for a card reminder state.
     * This allows testing the reminder logic without waiting 30 days.
     * 
     * @param stateId Reminder state ID
     * @param minutes Number of minutes from now (default: 5)
     * @return Updated reminder state info
     */
    @PostMapping("/set-due-date/{stateId}")
    public ResponseEntity<?> setDueDateInMinutes(
            @PathVariable UUID stateId,
            @RequestParam(defaultValue = "5") int minutes) {
        try {
            Optional<CardFeeReminderState> stateOpt = reminderStateRepository.findById(stateId);
            if (stateOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Reminder state not found: " + stateId));
            }

            CardFeeReminderState state = stateOpt.get();
            LocalDate now = LocalDate.now(ZONE);
            LocalDate newDueDate;
            
            // Since next_due_date is LocalDate (date only, no time), we need to handle minutes differently
            // For testing: if minutes < 1440 (24 hours), set to today (will trigger immediately)
            // Otherwise, convert minutes to days
            if (minutes < 1440) {
                // Less than 24 hours: set to today for immediate testing
                // Note: The reminder job runs daily at 8 AM, so setting to today means it will trigger
                // the next time the job runs. For testing, you can manually trigger the job.
                newDueDate = now;
            } else {
                // More than 24 hours: convert minutes to days
                int days = minutes / 1440;
                newDueDate = now.plusDays(days);
            }

            // Update next_due_date
            state.setNextDueDate(newDueDate);
            state.setReminderCount(0); // Reset reminder count for testing
            state.setLastRemindedAt(null); // Reset last reminded timestamp
            reminderStateRepository.save(state);

            log.info("✅ [TEST] Set next_due_date for reminder state {} to {} ({} minutes from now)",
                    stateId, newDueDate, minutes);

            Map<String, Object> response = new HashMap<>();
            response.put("stateId", stateId);
            response.put("cardType", state.getCardType());
            response.put("cardId", state.getCardId());
            response.put("oldDueDate", state.getCycleStartDate());
            response.put("newDueDate", newDueDate);
            response.put("minutesFromNow", minutes);
            response.put("message", String.format("Next due date set to %s (%d minutes from now). " +
                    "Reminder job will trigger notification when current date >= due date.", newDueDate, minutes));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error setting due date for reminder state {}: {}", stateId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to set due date: " + e.getMessage()));
        }
    }

    /**
     * Find reminder state by card ID and card type.
     * Useful for finding stateId after card is approved.
     * 
     * @param cardId Card ID
     * @param cardType Card type (RESIDENT, ELEVATOR, VEHICLE)
     * @return Reminder state info
     */
    @GetMapping("/find-by-card")
    public ResponseEntity<?> findByCard(
            @RequestParam UUID cardId,
            @RequestParam String cardType) {
        try {
            Optional<CardFeeReminderState> stateOpt = reminderStateRepository
                    .findByCardTypeAndCardId(cardType.toUpperCase(), cardId);
            
            if (stateOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Reminder state not found for card: " + cardId));
            }

            CardFeeReminderState state = stateOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("stateId", state.getId());
            response.put("cardType", state.getCardType());
            response.put("cardId", state.getCardId());
            response.put("nextDueDate", state.getNextDueDate());
            response.put("reminderCount", state.getReminderCount());
            response.put("lastRemindedAt", state.getLastRemindedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error finding reminder state for card {}: {}", cardId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to find reminder state: " + e.getMessage()));
        }
    }

    /**
     * Set next_due_date to today (immediately due) for testing.
     * 
     * @param stateId Reminder state ID
     * @return Updated reminder state info
     */
    @PostMapping("/set-due-today/{stateId}")
    public ResponseEntity<?> setDueToday(@PathVariable UUID stateId) {
        try {
            Optional<CardFeeReminderState> stateOpt = reminderStateRepository.findById(stateId);
            if (stateOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Reminder state not found: " + stateId));
            }

            CardFeeReminderState state = stateOpt.get();
            LocalDate today = LocalDate.now(ZONE);

            // Set next_due_date to today (immediately due)
            state.setNextDueDate(today);
            state.setReminderCount(0); // Reset reminder count for testing
            state.setLastRemindedAt(null); // Reset last reminded timestamp
            reminderStateRepository.save(state);

            log.info("✅ [TEST] Set next_due_date for reminder state {} to today ({})",
                    stateId, today);

            Map<String, Object> response = new HashMap<>();
            response.put("stateId", stateId);
            response.put("cardType", state.getCardType());
            response.put("cardId", state.getCardId());
            response.put("dueDate", today);
            response.put("message", "Next due date set to today. Reminder job will trigger notification immediately.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error setting due date to today for reminder state {}: {}", stateId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to set due date: " + e.getMessage()));
        }
    }

    /**
     * Manually trigger the reminder job to test notification sending.
     * This bypasses the scheduled job and runs immediately.
     * 
     * @return Job execution result
     */
    @PostMapping("/trigger-reminder-job")
    public ResponseEntity<?> triggerReminderJob() {
        try {
            log.info("🔔 [TEST] Manually triggering reminder job...");
            
            // Call the scheduler's executeReminderJob method directly
            reminderScheduler.executeReminderJob();

            log.info("✅ [TEST] Reminder job executed successfully");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reminder job executed successfully. Check logs for details.");
            response.put("timestamp", LocalDateTime.now(ZONE));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error executing reminder job: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to execute reminder job: " + e.getMessage()));
        }
    }

    /**
     * Get reminder state info by ID.
     * 
     * @param stateId Reminder state ID
     * @return Reminder state info
     */
    @GetMapping("/state/{stateId}")
    public ResponseEntity<?> getReminderState(@PathVariable UUID stateId) {
        try {
            Optional<CardFeeReminderState> stateOpt = reminderStateRepository.findById(stateId);
            if (stateOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Reminder state not found: " + stateId));
            }

            CardFeeReminderState state = stateOpt.get();
            LocalDate today = LocalDate.now(ZONE);
            long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, state.getNextDueDate());
            boolean isDue = state.getNextDueDate().isBefore(today) || state.getNextDueDate().isEqual(today);

            Map<String, Object> response = new HashMap<>();
            response.put("stateId", state.getId());
            response.put("cardType", state.getCardType());
            response.put("cardId", state.getCardId());
            response.put("unitId", state.getUnitId());
            response.put("residentId", state.getResidentId());
            response.put("cycleStartDate", state.getCycleStartDate());
            response.put("nextDueDate", state.getNextDueDate());
            response.put("reminderCount", state.getReminderCount());
            response.put("maxReminders", state.getMaxReminders());
            response.put("lastRemindedAt", state.getLastRemindedAt());
            response.put("today", today);
            response.put("daysUntilDue", daysUntilDue);
            response.put("isDue", isDue);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error getting reminder state {}: {}", stateId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to get reminder state: " + e.getMessage()));
        }
    }

    /**
     * List all reminder states (for testing/debugging).
     * 
     * @return List of reminder states
     */
    @GetMapping("/states")
    public ResponseEntity<?> listReminderStates() {
        try {
            var states = reminderStateRepository.findAll();
            LocalDate today = LocalDate.now(ZONE);

            var response = states.stream()
                    .map(state -> {
                        long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, state.getNextDueDate());
                        boolean isDue = state.getNextDueDate().isBefore(today) || state.getNextDueDate().isEqual(today);

                        Map<String, Object> info = new HashMap<>();
                        info.put("stateId", state.getId());
                        info.put("cardType", state.getCardType());
                        info.put("cardId", state.getCardId());
                        info.put("unitId", state.getUnitId());
                        info.put("residentId", state.getResidentId());
                        info.put("nextDueDate", state.getNextDueDate());
                        info.put("reminderCount", state.getReminderCount());
                        info.put("maxReminders", state.getMaxReminders());
                        info.put("daysUntilDue", daysUntilDue);
                        info.put("isDue", isDue);
                        return info;
                    })
                    .toList();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error listing reminder states: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to list reminder states: " + e.getMessage()));
        }
    }

    /**
     * Manually sync active cards into reminder state table.
     * This is useful when you have approved/paid cards but they haven't been synced yet.
     * 
     * @return Sync result
     */
    @PostMapping("/sync-cards")
    public ResponseEntity<?> syncActiveCards() {
        try {
            log.info("🔄 [TEST] Manually syncing active cards into reminder state...");
            
            reminderService.syncActiveCardsIntoReminderState();
            
            // Count how many states were created/updated
            var states = reminderStateRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Active cards synced successfully");
            response.put("totalReminderStates", states.size());
            response.put("timestamp", LocalDateTime.now(ZONE));
            
            log.info("✅ [TEST] Synced active cards. Total reminder states: {}", states.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error syncing active cards: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to sync active cards: " + e.getMessage()));
        }
    }

    /**
     * Create a test reminder state for testing purposes.
     * This creates a fake reminder state that you can use to test the reminder logic.
     * 
     * @param cardType Card type (RESIDENT, ELEVATOR, or VEHICLE)
     * @param cardId Card ID (will generate if not provided)
     * @param unitId Unit ID (required)
     * @param residentId Resident ID (required)
     * @param userId User ID (required)
     * @param apartmentNumber Apartment number (optional)
     * @param buildingName Building name (optional)
     * @param dueInDays Number of days until due (default: 0 = today)
     * @return Created reminder state info
     */
    @PostMapping("/create-test-state")
    public ResponseEntity<?> createTestReminderState(
            @RequestParam(required = false, defaultValue = "RESIDENT") String cardType,
            @RequestParam(required = false) UUID cardId,
            @RequestParam UUID unitId,
            @RequestParam UUID residentId,
            @RequestParam UUID userId,
            @RequestParam(required = false) String apartmentNumber,
            @RequestParam(required = false) String buildingName,
            @RequestParam(required = false, defaultValue = "0") int dueInDays) {
        try {
            // Generate card ID if not provided
            if (cardId == null) {
                cardId = UUID.randomUUID();
            }
            
            LocalDate today = LocalDate.now(ZONE);
            LocalDate cycleStart = today.minusDays(30 - dueInDays); // Calculate cycle start to get desired due date
            LocalDate nextDue = today.plusDays(dueInDays);
            
            // Check if state already exists
            Optional<CardFeeReminderState> existing = reminderStateRepository
                    .findByCardTypeAndCardId(cardType, cardId);
            
            CardFeeReminderState state;
            if (existing.isPresent()) {
                state = existing.get();
                state.setNextDueDate(nextDue);
                state.setCycleStartDate(cycleStart);
                state.setReminderCount(0);
                state.setLastRemindedAt(null);
                log.info("📝 [TEST] Updating existing reminder state for {} card {}", cardType, cardId);
            } else {
                state = CardFeeReminderState.builder()
                        .cardType(cardType)
                        .cardId(cardId)
                        .unitId(unitId)
                        .residentId(residentId)
                        .userId(userId)
                        .apartmentNumber(apartmentNumber != null && !apartmentNumber.isBlank() ? apartmentNumber : "TEST")
                        .buildingName(buildingName != null && !buildingName.isBlank() ? buildingName : "Tòa TEST")
                        .cycleStartDate(cycleStart)
                        .nextDueDate(nextDue)
                        .reminderCount(0)
                        .maxReminders(6)
                        .lastRemindedAt(null)
                        .build();
                log.info("➕ [TEST] Creating new test reminder state for {} card {}", cardType, cardId);
            }
            
            reminderStateRepository.save(state);
            
            Map<String, Object> response = new HashMap<>();
            response.put("stateId", state.getId());
            response.put("cardType", state.getCardType());
            response.put("cardId", state.getCardId());
            response.put("unitId", state.getUnitId());
            response.put("residentId", state.getResidentId());
            response.put("cycleStartDate", state.getCycleStartDate());
            response.put("nextDueDate", state.getNextDueDate());
            response.put("reminderCount", state.getReminderCount());
            response.put("maxReminders", state.getMaxReminders());
            response.put("daysUntilDue", dueInDays);
            response.put("message", String.format("Test reminder state created. Due in %d days (%s)", 
                    dueInDays, nextDue));
            
            log.info("✅ [TEST] Test reminder state created/updated: stateId={}, dueDate={}", 
                    state.getId(), nextDue);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [TEST] Error creating test reminder state: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to create test reminder state: " + e.getMessage()));
        }
    }
}

