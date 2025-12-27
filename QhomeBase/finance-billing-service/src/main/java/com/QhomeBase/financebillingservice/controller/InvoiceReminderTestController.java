package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.ResidentRepository;
import com.QhomeBase.financebillingservice.service.InvoiceReminderService;
import com.QhomeBase.financebillingservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices/test")
@RequiredArgsConstructor
@Slf4j
public class InvoiceReminderTestController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static boolean hasTriggeredOnStartup = false;

    private final InvoiceReminderService invoiceReminderService;
    private final InvoiceRepository invoiceRepository;
    private final ResidentRepository residentRepository;
    private final JwtUtil jwtUtil;
    private final BaseServiceClient baseServiceClient;

    /**
     * GET endpoint để kiểm tra trạng thái invoice và điều kiện trigger
     * Kiểm tra xem invoice có đủ điều kiện để gửi reminder hoặc mark as UNPAID không
     */
    @GetMapping("/check-status")
    public ResponseEntity<Map<String, Object>> checkInvoiceStatus(HttpServletRequest request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cannot get userId from authentication",
                        "message", "Please ensure you are logged in"
                ));
            }

            Optional<UUID> residentIdOpt = residentRepository.findResidentIdByUserId(userId);
            if (residentIdOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Resident not found",
                        "message", "User does not have a resident profile"
                ));
            }
            UUID residentId = residentIdOpt.get();

            UUID unitId = baseServiceClient.getUnitIdFromResidentId(residentId);
            if (unitId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Unit not found",
                        "message", "Resident does not belong to any unit"
                ));
            }

            List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitId).stream()
                    .filter(inv -> inv.getStatus() == InvoiceStatus.PUBLISHED)
                    .toList();

            OffsetDateTime now = OffsetDateTime.now(ZONE);
            int currentDay = now.getDayOfMonth();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();

            List<Map<String, Object>> invoiceStatuses = new java.util.ArrayList<>();
            for (Invoice invoice : invoices) {
                Map<String, Object> status = new HashMap<>();
                status.put("invoiceId", invoice.getId().toString());
                status.put("invoiceCode", invoice.getCode());
                status.put("status", invoice.getStatus().name());
                status.put("issuedAt", invoice.getIssuedAt() != null ? invoice.getIssuedAt().toString() : null);
                status.put("reminderCount", invoice.getReminderCount() != null ? invoice.getReminderCount() : 0);
                status.put("lastReminderAt", invoice.getLastReminderAt() != null ? invoice.getLastReminderAt().toString() : null);
                
                if (invoice.getIssuedAt() != null) {
                    OffsetDateTime issuedAt = invoice.getIssuedAt();
                    boolean isCurrentMonth = issuedAt.getYear() == currentYear && issuedAt.getMonthValue() == currentMonth;
                    boolean isDay15 = issuedAt.getDayOfMonth() == 15;
                    status.put("isCurrentMonth", isCurrentMonth);
                    status.put("isDay15", isDay15);
                    
                    if (isCurrentMonth && isDay15) {
                        if (currentDay > 22) {
                            status.put("shouldMarkUnpaid", true);
                            status.put("shouldSendReminder", false);
                        } else if (currentDay >= 16 && currentDay <= 22) {
                            status.put("shouldMarkUnpaid", false);
                            boolean canSend = invoice.getLastReminderAt() == null 
                                    ? now.isAfter(issuedAt.plusHours(24)) || now.isEqual(issuedAt.plusHours(24))
                                    : now.isAfter(invoice.getLastReminderAt().plusHours(24)) || now.isEqual(invoice.getLastReminderAt().plusHours(24));
                            status.put("shouldSendReminder", canSend);
                        } else {
                            status.put("shouldMarkUnpaid", false);
                            status.put("shouldSendReminder", false);
                        }
                    } else {
                        status.put("shouldMarkUnpaid", false);
                        status.put("shouldSendReminder", false);
                    }
                }
                
                invoiceStatuses.add(status);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Invoice status check",
                    "residentId", residentId.toString(),
                    "unitId", unitId.toString(),
                    "currentDay", currentDay,
                    "currentMonth", currentMonth,
                    "currentYear", currentYear,
                    "totalInvoices", invoices.size(),
                    "invoices", invoiceStatuses
            ));
        } catch (Exception e) {
            log.error("Error checking invoice status: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * POST endpoint để trigger reminder cho invoice của user đang đăng nhập
     * Gọi endpoint này sau khi login để trigger reminder
     */
    @PostMapping("/trigger-reminder")
    public ResponseEntity<Map<String, Object>> triggerReminderForCurrentUser(HttpServletRequest request) {
        try {
            // Get userId from JWT token
            UUID userId = getUserIdFromRequest(request);
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cannot get userId from authentication",
                        "message", "Please ensure you are logged in"
                ));
            }

            // Get residentId from userId
            Optional<UUID> residentIdOpt = residentRepository.findResidentIdByUserId(userId);
            if (residentIdOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Resident not found",
                        "message", "User does not have a resident profile"
                ));
            }
            UUID residentId = residentIdOpt.get();

            // Get unitId from residentId
            UUID unitId = baseServiceClient.getUnitIdFromResidentId(residentId);
            if (unitId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Unit not found",
                        "message", "Resident does not belong to any unit"
                ));
            }

            // Find PUBLISHED invoices for this unit
            List<Invoice> invoices = invoiceRepository.findByPayerUnitId(unitId).stream()
                    .filter(inv -> inv.getStatus() == InvoiceStatus.PUBLISHED)
                    .toList();

            if (invoices.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No PUBLISHED invoices found for your unit",
                        "residentId", residentId.toString(),
                        "unitId", unitId.toString(),
                        "invoicesProcessed", 0
                ));
            }

            OffsetDateTime now = OffsetDateTime.now(ZONE);
            int currentDay = now.getDayOfMonth();
            int currentMonth = now.getMonthValue();
            int currentYear = now.getYear();

            int remindersSent = 0;
            int unpaidMarked = 0;
            List<Map<String, Object>> results = new java.util.ArrayList<>();

            for (Invoice invoice : invoices) {
                try {
                    // Check if invoice was issued on day 15 of current month
                    if (invoice.getIssuedAt() == null) {
                        continue;
                    }
                    OffsetDateTime issuedAt = invoice.getIssuedAt();
                    if (issuedAt.getYear() != currentYear || issuedAt.getMonthValue() != currentMonth) {
                        continue; // Not in current month
                    }
                    if (issuedAt.getDayOfMonth() != 15) {
                        continue; // Not issued on day 15
                    }

                    Map<String, Object> invoiceResult = new HashMap<>();
                    invoiceResult.put("invoiceId", invoice.getId().toString());
                    invoiceResult.put("invoiceCode", invoice.getCode());
                    invoiceResult.put("status", invoice.getStatus().name());

                    // Check if should mark as UNPAID (after day 22)
                    if (currentDay > 22) {
                        if (invoice.getStatus() == InvoiceStatus.PUBLISHED) {
                            invoiceReminderService.markInvoiceAsUnpaid(invoice);
                            invoiceResult.put("action", "marked_as_unpaid");
                            unpaidMarked++;
                        } else {
                            invoiceResult.put("action", "already_unpaid");
                        }
                    } else if (currentDay >= 16 && currentDay <= 22) {
                        // Check if should send reminder
                        boolean shouldSendReminder = false;
                        if (invoice.getLastReminderAt() == null) {
                            // Never reminded, check if 24h passed since issued
                            OffsetDateTime firstReminderTime = invoice.getIssuedAt().plusHours(24);
                            if (now.isAfter(firstReminderTime) || now.isEqual(firstReminderTime)) {
                                shouldSendReminder = true;
                            }
                        } else {
                            // Check if 24h passed since last reminder
                            OffsetDateTime nextReminderTime = invoice.getLastReminderAt().plusHours(24);
                            if (now.isAfter(nextReminderTime) || now.isEqual(nextReminderTime)) {
                                shouldSendReminder = true;
                            }
                        }

                        if (shouldSendReminder) {
                            invoiceReminderService.sendReminder(invoice);
                            invoiceResult.put("action", "reminder_sent");
                            invoiceResult.put("day", currentDay);
                            invoiceResult.put("isFinalWarning", currentDay == 22);
                            remindersSent++;
                        } else {
                            invoiceResult.put("action", "skip_reminder");
                            invoiceResult.put("reason", "24 hours not passed since last reminder");
                        }
                    } else {
                        invoiceResult.put("action", "skip");
                        invoiceResult.put("reason", "Current day is " + currentDay + ", reminders only sent from day 16-22");
                    }

                    results.add(invoiceResult);
                } catch (Exception e) {
                    log.error("Error processing invoice {}: {}", invoice.getId(), e.getMessage(), e);
                    results.add(Map.of(
                            "invoiceId", invoice.getId().toString(),
                            "error", e.getMessage()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Reminder trigger completed",
                    "residentId", residentId.toString(),
                    "unitId", unitId.toString(),
                    "currentDay", currentDay,
                    "remindersSent", remindersSent,
                    "unpaidMarked", unpaidMarked,
                    "totalInvoices", invoices.size(),
                    "results", results
            ));
        } catch (Exception e) {
            log.error("Error triggering reminder: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Auto-trigger reminder when service starts (only once)
     * Tự động trigger reminder cho tất cả invoices đủ điều kiện khi service start
     * Không cần user cụ thể - sẽ xử lý tất cả invoices trong hệ thống
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (hasTriggeredOnStartup) {
            return;
        }
        hasTriggeredOnStartup = true;
        
        log.info("🔄 [InvoiceReminderTest] Service started - Auto-triggering reminders for all eligible invoices...");
        
        try {
            // Trigger reminders for all invoices that need reminder
            List<Invoice> invoicesNeedingReminder = invoiceReminderService.findInvoicesNeedingReminder();
            int remindersSent = 0;
            for (Invoice invoice : invoicesNeedingReminder) {
                try {
                    invoiceReminderService.sendReminder(invoice);
                    remindersSent++;
                } catch (Exception e) {
                    log.error("Error sending reminder for invoice {}: {}", invoice.getId(), e.getMessage());
                }
            }
            
            // Mark invoices as UNPAID if needed
            List<Invoice> invoicesNeedingUnpaid = invoiceReminderService.findInvoicesNeedingUnpaidStatus();
            int unpaidMarked = 0;
            for (Invoice invoice : invoicesNeedingUnpaid) {
                try {
                    invoiceReminderService.markInvoiceAsUnpaid(invoice);
                    unpaidMarked++;
                } catch (Exception e) {
                    log.error("Error marking invoice {} as UNPAID: {}", invoice.getId(), e.getMessage());
                }
            }
            
            log.info("✅ [InvoiceReminderTest] Auto-trigger completed: {} reminders sent, {} invoices marked as UNPAID", 
                    remindersSent, unpaidMarked);
        } catch (Exception e) {
            log.error("❌ [InvoiceReminderTest] Error in auto-trigger on startup: {}", e.getMessage(), e);
        }
    }

    /**
     * Get userId from request (JWT token)
     */
    private UUID getUserIdFromRequest(HttpServletRequest request) {
        // Get from JWT token in Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            UUID userId = jwtUtil.getUserIdFromHeader(authHeader);
            if (userId != null) {
                return userId;
            }
        }

        return null;
    }
}
