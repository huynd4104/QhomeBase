package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceLine;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceLineRepository;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceReminderService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int REMINDER_INTERVAL_HOURS = 24;
    private static final int INVOICE_ISSUE_DAY = 15; // Ngày 15: Admin gửi invoice
    private static final int REMINDER_START_DAY = 16; // Ngày 16: Bắt đầu gửi reminder
    private static final int FINAL_WARNING_DAY = 22; // Ngày 22: Cảnh báo cuối (cắt điện/nước)

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final NotificationClient notificationClient;
    private final BaseServiceClient baseServiceClient;

    @Value("${invoice.reminder.enabled:true}")
    private boolean remindersEnabled;

    public InvoiceRepository getInvoiceRepository() {
        return invoiceRepository;
    }

    /**
     * Find invoices that need reminder:
     * Logic mới: Dựa trên ngày trong tháng
     * - Ngày 15: Admin gửi invoice
     * - Từ ngày 16 đến ngày 22: Gửi reminder mỗi 24 giờ
     * - Ngày 22: Cảnh báo cuối (cắt điện/nước)
     * 
     * Điều kiện:
     * 1. Status = PUBLISHED (chưa thanh toán)
     * 2. Có payerUnitId (để lấy tất cả residents trong unit)
     * 3. Invoice được tạo trong tháng hiện tại (issued_at trong tháng này)
     * 4. Ngày hiện tại >= 16 và <= 22
     * 5. Đã qua 24 giờ từ lần nhắc cuối (last_reminder_at + 24h <= now) hoặc chưa nhắc lần nào
     */
    @Transactional(readOnly = true)
    public List<Invoice> findInvoicesNeedingReminder() {
        if (!remindersEnabled) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now(ZONE);
        int currentDay = now.getDayOfMonth();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Chỉ gửi reminder từ ngày 16 đến ngày 22
        if (currentDay < REMINDER_START_DAY || currentDay > FINAL_WARNING_DAY) {
            return List.of();
        }

        // Find invoices that:
        // 1. Status = PUBLISHED
        // 2. Issued trong tháng hiện tại
        // 3. Có payerUnitId (để lấy tất cả residents trong unit)
        List<Invoice> allPublished = invoiceRepository.findByStatus(InvoiceStatus.PUBLISHED);
        
        return allPublished.stream()
                .filter(invoice -> {
                    // Skip invoices without payerUnitId (cần để lấy tất cả residents trong unit)
                    if (invoice.getPayerUnitId() == null) {
                        return false;
                    }
                    
                    // Check if issued in current month
                    if (invoice.getIssuedAt() == null) {
                        return false;
                    }
                    OffsetDateTime issuedAt = invoice.getIssuedAt();
                    if (issuedAt.getYear() != currentYear || issuedAt.getMonthValue() != currentMonth) {
                        return false; // Invoice không phải trong tháng hiện tại
                    }

                    // Check if issued on day 15 (admin gửi invoice vào ngày 15)
                    int issuedDay = issuedAt.getDayOfMonth();
                    if (issuedDay != INVOICE_ISSUE_DAY) {
                        return false; // Invoice không được gửi vào ngày 15
                    }

                    // Check if last reminder was at least 24 hours ago (or never reminded)
                    if (invoice.getLastReminderAt() != null) {
                        OffsetDateTime nextReminderTime = invoice.getLastReminderAt().plusHours(REMINDER_INTERVAL_HOURS);
                        if (now.isBefore(nextReminderTime)) {
                            return false; // Chưa đến 24 giờ từ lần nhắc cuối
                        }
                    }

                    return true;
                })
                .toList();
    }

    /**
     * Find invoices that need to be marked as UNPAID:
     * Logic mới: Sau ngày 22 trong tháng
     * - Invoice được gửi vào ngày 15
     * - Ngày hiện tại > 22 (sau ngày 22)
     * - Status vẫn là PUBLISHED (chưa thanh toán)
     */
    @Transactional(readOnly = true)
    public List<Invoice> findInvoicesNeedingUnpaidStatus() {
        if (!remindersEnabled) {
            return List.of();
        }

        OffsetDateTime now = OffsetDateTime.now(ZONE);
        int currentDay = now.getDayOfMonth();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Chỉ đánh dấu UNPAID sau ngày 22
        if (currentDay <= FINAL_WARNING_DAY) {
            return List.of();
        }

        List<Invoice> allPublished = invoiceRepository.findByStatus(InvoiceStatus.PUBLISHED);
        
        return allPublished.stream()
                .filter(invoice -> {
                    // Check if issued in current month
                    if (invoice.getIssuedAt() == null) {
                        return false;
                    }
                    OffsetDateTime issuedAt = invoice.getIssuedAt();
                    if (issuedAt.getYear() != currentYear || issuedAt.getMonthValue() != currentMonth) {
                        return false; // Invoice không phải trong tháng hiện tại
                    }

                    // Check if issued on day 15
                    int issuedDay = issuedAt.getDayOfMonth();
                    if (issuedDay != INVOICE_ISSUE_DAY) {
                        return false; // Invoice không được gửi vào ngày 15
                    }

                    return true;
                })
                .toList();
    }

    /**
     * Send reminder notification for an invoice
     * Logic mới: Gửi cho tất cả residents trong unit (không chỉ OWNER)
     */
    @Transactional
    public void sendReminder(Invoice invoice) {
        if (invoice.getPayerUnitId() == null) {
            log.debug("ℹ️ [InvoiceReminderService] Skipping reminder: payerUnitId is null for invoice {}", invoice.getId());
            return;
        }

        try {
            // Get buildingId from unitId
            UUID buildingId = null;
            try {
                BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                if (unitInfo != null && unitInfo.getBuildingId() != null) {
                    buildingId = unitInfo.getBuildingId();
                }
            } catch (Exception e) {
                log.warn("⚠️ [InvoiceReminderService] Failed to get buildingId from unitId {}: {}", 
                        invoice.getPayerUnitId(), e.getMessage());
            }

            // Get all residents in the unit (via household)
            List<UUID> residentIds = new java.util.ArrayList<>();
            try {
                BaseServiceClient.ServiceInfo.HouseholdInfo household = baseServiceClient.getCurrentHouseholdByUnitId(invoice.getPayerUnitId());
                if (household != null && household.getId() != null) {
                    List<BaseServiceClient.ServiceInfo.HouseholdMemberInfo> members = baseServiceClient.getActiveMembersByHouseholdId(household.getId());
                    for (BaseServiceClient.ServiceInfo.HouseholdMemberInfo member : members) {
                        if (member.getResidentId() != null) {
                            residentIds.add(member.getResidentId());
                        }
                    }
                    // Also include primary resident if not already in the list
                    if (household.getPrimaryResidentId() != null && !residentIds.contains(household.getPrimaryResidentId())) {
                        residentIds.add(household.getPrimaryResidentId());
                    }
                } else {
                    // Fallback: if no household found, use payerResidentId
                    if (invoice.getPayerResidentId() != null) {
                        residentIds.add(invoice.getPayerResidentId());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [InvoiceReminderService] Failed to get residents for unit {}: {}", 
                        invoice.getPayerUnitId(), e.getMessage());
                // Fallback: use payerResidentId
                if (invoice.getPayerResidentId() != null) {
                    residentIds.add(invoice.getPayerResidentId());
                }
            }

            if (residentIds.isEmpty()) {
                log.warn("⚠️ [InvoiceReminderService] No residents found for unit {}, skipping reminder", invoice.getPayerUnitId());
                return;
            }

            // Calculate total amount
            BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoice.getId()).stream()
                    .map(InvoiceLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Format amount
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            currencyFormat.setMaximumFractionDigits(0);
            String amountText = currencyFormat.format(totalAmount);

            // Determine service type (điện/nước) from invoice lines
            String serviceType = determineServiceType(invoice.getId());
            String serviceTypeText = serviceType != null ? serviceType : "dịch vụ";

            // Build notification message
            String invoiceCode = invoice.getCode() != null ? invoice.getCode() : invoice.getId().toString();
            OffsetDateTime now = OffsetDateTime.now(ZONE);
            int currentDay = now.getDayOfMonth();
            
            String title;
            String message;
            
            // Ngày 22: Cảnh báo cuối (cắt điện/nước)
            if (currentDay == FINAL_WARNING_DAY) {
                title = "⚠️ CẢNH BÁO CUỐI: Hóa đơn chưa thanh toán - " + invoiceCode;
                message = String.format(
                        "CẢNH BÁO: Bạn có hóa đơn %s chưa thanh toán với số tiền %s. Hạn thanh toán: %s. "
                        + "Nếu không thanh toán trong vòng 24 giờ tới, dịch vụ %s sẽ bị cắt. Vui lòng thanh toán ngay!",
                        invoiceCode,
                        amountText,
                        invoice.getDueDate() != null 
                                ? invoice.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                                : "N/A",
                        serviceTypeText);
            } else {
                // Các lần nhắc thông thường (ngày 16-21)
                title = "Nhắc nhở thanh toán hóa đơn - " + invoiceCode;
                message = String.format(
                        "Bạn có hóa đơn %s chưa thanh toán với số tiền %s. Hạn thanh toán: %s. "
                        + "Vui lòng thanh toán sớm để tránh gián đoạn dịch vụ.",
                        invoiceCode,
                        amountText,
                        invoice.getDueDate() != null 
                                ? invoice.getDueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) 
                                : "N/A");
            }

            // Prepare data payload
            Map<String, String> data = new HashMap<>();
            data.put("invoiceId", invoice.getId().toString());
            data.put("invoiceCode", invoiceCode);
            data.put("amount", totalAmount.toString());
            data.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
            data.put("serviceType", serviceType != null ? serviceType : "");
            data.put("isFinalWarning", String.valueOf(currentDay == FINAL_WARNING_DAY));

            // Send notification to all residents in the unit
            int sentCount = 0;
            for (UUID residentId : residentIds) {
                try {
                    notificationClient.sendResidentNotification(
                            residentId,
                            buildingId,
                            "BILL",
                            title,
                            message,
                            invoice.getId(),
                            "INVOICE_REMINDER",
                            data
                    );
                    sentCount++;
                } catch (Exception e) {
                    log.warn("⚠️ [InvoiceReminderService] Failed to send reminder to residentId={}: {}", 
                            residentId, e.getMessage());
                }
            }

            // Update reminder count and last reminder time
            int reminderCount = invoice.getReminderCount() != null ? invoice.getReminderCount() : 0;
            invoice.setReminderCount(reminderCount + 1);
            invoice.setLastReminderAt(now);
            invoiceRepository.save(invoice);

            log.info("✅ [InvoiceReminderService] Sent reminder (day {}) {} to {} residents in unit {}, invoiceId={}", 
                    currentDay,
                    currentDay == FINAL_WARNING_DAY ? "(FINAL WARNING)" : "",
                    sentCount, invoice.getPayerUnitId(), invoice.getId());
        } catch (Exception e) {
            log.error("❌ [InvoiceReminderService] Failed to send reminder for invoiceId={}: {}", 
                    invoice.getId(), e.getMessage(), e);
        }
    }

    /**
     * Determine service type (điện/nước) from invoice lines
     * Returns "điện" if invoice contains ELECTRIC service, "nước" if WATER, or null
     */
    private String determineServiceType(UUID invoiceId) {
        List<InvoiceLine> lines = invoiceLineRepository.findByInvoiceId(invoiceId);
        for (InvoiceLine line : lines) {
            String serviceCode = line.getServiceCode();
            if (serviceCode != null) {
                String normalized = serviceCode.toUpperCase().trim();
                if ("ELECTRIC".equals(normalized) || "ELECTRICITY".equals(normalized)) {
                    return "điện";
                } else if ("WATER".equals(normalized)) {
                    return "nước";
                }
            }
        }
        return null; // Unknown service type
    }

    /**
     * Mark invoice as UNPAID status
     * Logic mới: Sau ngày 22 trong tháng
     * Gửi notification cho tất cả residents trong unit
     */
    @Transactional
    public void markInvoiceAsUnpaid(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.UNPAID) {
            log.info("ℹ️ [InvoiceReminderService] Invoice {} is already UNPAID, skipping.", invoice.getId());
            return;
        }
        
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoiceRepository.save(invoice);
        log.info("✅ [InvoiceReminderService] Invoice {} marked as UNPAID.", invoice.getId());

        // Send notification about being marked as UNPAID to all residents in unit
        try {
            if (invoice.getPayerUnitId() == null) {
                log.warn("⚠️ [InvoiceReminderService] payerUnitId is null, cannot send UNPAID notification");
                return;
            }

            // Get buildingId from unitId
            UUID buildingId = null;
            try {
                BaseServiceClient.UnitInfo unitInfo = baseServiceClient.getUnitById(invoice.getPayerUnitId());
                if (unitInfo != null && unitInfo.getBuildingId() != null) {
                    buildingId = unitInfo.getBuildingId();
                }
            } catch (Exception e) {
                log.warn("⚠️ [InvoiceReminderService] Failed to get buildingId from unitId {}: {}", 
                        invoice.getPayerUnitId(), e.getMessage());
            }

            // Get all residents in the unit (via household)
            List<UUID> residentIds = new java.util.ArrayList<>();
            try {
                BaseServiceClient.ServiceInfo.HouseholdInfo household = baseServiceClient.getCurrentHouseholdByUnitId(invoice.getPayerUnitId());
                if (household != null && household.getId() != null) {
                    List<BaseServiceClient.ServiceInfo.HouseholdMemberInfo> members = baseServiceClient.getActiveMembersByHouseholdId(household.getId());
                    for (BaseServiceClient.ServiceInfo.HouseholdMemberInfo member : members) {
                        if (member.getResidentId() != null) {
                            residentIds.add(member.getResidentId());
                        }
                    }
                    // Also include primary resident if not already in the list
                    if (household.getPrimaryResidentId() != null && !residentIds.contains(household.getPrimaryResidentId())) {
                        residentIds.add(household.getPrimaryResidentId());
                    }
                } else {
                    // Fallback: if no household found, use payerResidentId
                    if (invoice.getPayerResidentId() != null) {
                        residentIds.add(invoice.getPayerResidentId());
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [InvoiceReminderService] Failed to get residents for unit {}: {}", 
                        invoice.getPayerUnitId(), e.getMessage());
                // Fallback: use payerResidentId
                if (invoice.getPayerResidentId() != null) {
                    residentIds.add(invoice.getPayerResidentId());
                }
            }

            if (residentIds.isEmpty()) {
                log.warn("⚠️ [InvoiceReminderService] No residents found for unit {}, skipping UNPAID notification", invoice.getPayerUnitId());
                return;
            }

            // Calculate total amount
            BigDecimal totalAmount = invoiceLineRepository.findByInvoiceId(invoice.getId()).stream()
                    .map(InvoiceLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String invoiceCode = invoice.getCode() != null ? invoice.getCode() : invoice.getId().toString();
            String title = "Hóa đơn đã chuyển sang trạng thái CHƯA THANH TOÁN - " + invoiceCode;
            String message = String.format(
                    "Hóa đơn %s của bạn đã chuyển sang trạng thái CHƯA THANH TOÁN do quá hạn và chưa thanh toán. " +
                    "Vui lòng thanh toán ngay để khôi phục dịch vụ.",
                    invoiceCode);
            
            Map<String, String> data = new HashMap<>();
            data.put("invoiceId", invoice.getId().toString());
            data.put("invoiceCode", invoiceCode);
            data.put("status", InvoiceStatus.UNPAID.name());
            data.put("amount", totalAmount.toString());

            // Send notification to all residents in the unit
            int sentCount = 0;
            for (UUID residentId : residentIds) {
                try {
                    notificationClient.sendResidentNotification(
                            residentId,
                            buildingId,
                            "BILL",
                            title,
                            message,
                            invoice.getId(),
                            "INVOICE_UNPAID",
                            data
                    );
                    sentCount++;
                } catch (Exception e) {
                    log.warn("⚠️ [InvoiceReminderService] Failed to send UNPAID notification to residentId={}: {}", 
                            residentId, e.getMessage());
                }
            }

            log.info("✅ [InvoiceReminderService] Sent UNPAID notification to {} residents in unit {}, invoiceId={}", 
                    sentCount, invoice.getPayerUnitId(), invoice.getId());
        } catch (Exception e) {
            log.error("❌ [InvoiceReminderService] Failed to send UNPAID notification for invoiceId={}: {}", 
                    invoice.getId(), e.getMessage(), e);
        }
    }
}

