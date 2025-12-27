package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.model.BillingCycle;
import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.repository.BillingCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledBillingService {
    
    private final BillingCycleRepository billingCycleRepository;
    private final InvoiceReminderService invoiceReminderService;
    
    /**
     * Scheduled job to create billing cycles automatically
     * Runs on the 1st day of every month at 00:00 AM
     * Cron: "0 0 0 1 * ?" = second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void createMonthlyBillingCycles() {
        log.info("🔄 Starting scheduled monthly billing cycle creation...");
        
        LocalDate today = LocalDate.now();
        LocalDate periodFrom = today.withDayOfMonth(1);
        LocalDate periodTo = today.withDayOfMonth(today.lengthOfMonth());
        
        String cycleName = "Tháng " + today.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        
        log.info("📅 Creating billing cycle: {} (From: {} - To: {})", 
                cycleName, periodFrom, periodTo);
        
        // TODO: Implement logic to:
        // 1. Get all active tenants from base-service
        // 2. For each tenant, create billing cycle
        // 3. Get all active vehicles for each tenant
        // 4. Group by unit/resident
        // 5. Create invoices with invoice lines
        
        log.warn("⚠️ Scheduled billing cycle creation is not fully implemented yet.");
        log.info("✅ Scheduled billing cycle creation completed");
    }

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateLateFees() {
        log.info("🔄 Starting scheduled late fee calculation...");
        
        LocalDate today = LocalDate.now();
        
        log.info("📅 Calculating late fees for invoices overdue as of: {}", today);
        
        log.warn("⚠️ Late fee calculation is not fully implemented yet.");
        log.info("✅ Late fee calculation completed");
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void sendPaymentReminders() {
        log.info("🔄 Starting scheduled payment reminder sending...");
        
        LocalDate today = LocalDate.now();
        LocalDate reminderDate = today.plusDays(5); // 5 days before due
        
        log.info("📧 Sending reminders for invoices due on: {}", reminderDate);
        
        log.warn("⚠️ Payment reminder sending is not fully implemented yet.");
        log.info("✅ Payment reminder sending completed");
    }

    /**
     * Scheduled job to send invoice payment reminders
     * Runs every hour to check for invoices that need reminder
     * Logic:
     * 1. Find invoices with status = PUBLISHED
     * 2. Check if 24 hours have passed since invoice was issued
     * 3. Check if 24 hours have passed since last reminder (or never reminded)
     * 4. Check if reminder count < 7
     * 5. Send notification and update reminder count
     */
    @Scheduled(cron = "${invoice.reminder.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendInvoicePaymentReminders() {
        log.info("🔄 Starting scheduled invoice payment reminder sending...");
        
        try {
            List<Invoice> invoicesNeedingReminder = invoiceReminderService.findInvoicesNeedingReminder();
            
            if (invoicesNeedingReminder.isEmpty()) {
                log.debug("ℹ️ [InvoiceReminderJob] No invoices need reminder at this time");
                return;
            }
            
            log.info("📧 Found {} invoices that need reminder", invoicesNeedingReminder.size());
            
            int sentCount = 0;
            for (Invoice invoice : invoicesNeedingReminder) {
                try {
                    // Double-check status before sending (might have been paid between query and now)
                    Invoice currentInvoice = invoiceReminderService.getInvoiceRepository().findById(invoice.getId())
                            .orElse(null);
                    if (currentInvoice == null || currentInvoice.getStatus() != com.QhomeBase.financebillingservice.model.InvoiceStatus.PUBLISHED) {
                        log.debug("ℹ️ [InvoiceReminderJob] Invoice {} status changed, skipping", invoice.getId());
                        continue;
                    }
                    
                    invoiceReminderService.sendReminder(currentInvoice);
                    sentCount++;
                } catch (Exception e) {
                    log.error("❌ [InvoiceReminderJob] Failed to send reminder for invoice {}: {}", 
                            invoice.getId(), e.getMessage(), e);
                }
            }
            
            log.info("✅ [InvoiceReminderJob] Sent {} reminders out of {} invoices that needed reminder", 
                    sentCount, invoicesNeedingReminder.size());
        } catch (Exception e) {
            log.error("❌ [InvoiceReminderJob] Error in scheduled invoice reminder job", e);
        }
    }

    /**
     * Scheduled job to mark invoices as UNPAID
     * Runs every hour to check for invoices that need to be marked as UNPAID
     * Logic:
     * 1. Find invoices with status = PUBLISHED
     * 2. Check if reminder count = 4 (3 nhắc + 1 cảnh báo cuối)
     * 3. Check if 24 hours have passed since final warning
     * 4. Mark as UNPAID
     */
    @Scheduled(cron = "${invoice.reminder.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void markInvoicesAsUnpaid() {
        log.info("🔄 Starting scheduled invoice UNPAID status marking...");
        
        try {
            List<Invoice> invoicesNeedingUnpaid = invoiceReminderService.findInvoicesNeedingUnpaidStatus();
            
            if (invoicesNeedingUnpaid.isEmpty()) {
                log.debug("ℹ️ [InvoiceUnpaidJob] No invoices need to be marked as UNPAID at this time");
                return;
            }
            
            log.info("📧 Found {} invoices that need to be marked as UNPAID", invoicesNeedingUnpaid.size());
            
            int markedCount = 0;
            for (Invoice invoice : invoicesNeedingUnpaid) {
                try {
                    // Double-check status before marking (might have been paid between query and now)
                    Invoice currentInvoice = invoiceReminderService.getInvoiceRepository().findById(invoice.getId())
                            .orElse(null);
                    if (currentInvoice == null || currentInvoice.getStatus() != com.QhomeBase.financebillingservice.model.InvoiceStatus.PUBLISHED) {
                        log.debug("ℹ️ [InvoiceUnpaidJob] Invoice {} status changed, skipping", invoice.getId());
                        continue;
                    }
                    
                    invoiceReminderService.markInvoiceAsUnpaid(currentInvoice);
                    markedCount++;
                } catch (Exception e) {
                    log.error("❌ [InvoiceUnpaidJob] Failed to mark invoice {} as UNPAID: {}", 
                            invoice.getId(), e.getMessage(), e);
                }
            }
            
            log.info("✅ [InvoiceUnpaidJob] Marked {} invoices as UNPAID out of {} invoices that needed marking", 
                    markedCount, invoicesNeedingUnpaid.size());
        } catch (Exception e) {
            log.error("❌ [InvoiceUnpaidJob] Error in scheduled invoice UNPAID marking job", e);
        }
    }
}




