package com.QhomeBase.financebillingservice.jobs;

import com.QhomeBase.financebillingservice.model.Invoice;
import com.QhomeBase.financebillingservice.model.InvoiceStatus;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DISABLED: Invoice payment expiry job.
 * 
 * For invoices (tiền điện, tiền nước), payments should NOT be auto-expired.
 * The system only checks if payment is successful and updates status to PAID.
 * If user doesn't complete payment, the invoice status remains unchanged.
 * 
 * This job is kept for reference but is disabled. Only card registrations use payment expiry logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VnpayPaymentExpiryJob {

    private final InvoiceRepository invoiceRepository;

    @Value("${vnpay.payment.timeout-minutes:10}")
    private int timeoutMinutes;

    @Value("${vnpay.payment.sweep-interval-ms:60000}")
    private long sweepIntervalMs;

    /**
     * DISABLED: Invoice payments should not be auto-expired.
     * For invoices (tiền điện, tiền nước), the system only checks if payment is successful
     * and updates status to PAID. If user doesn't complete payment, status remains unchanged.
     * 
     * This job is kept for reference but is disabled.
     */
    // @Scheduled(fixedDelayString = "${vnpay.payment.sweep-interval-ms:60000}")
    @Transactional
    public void expirePendingVnpayPayments() {
        // DISABLED: Invoice payments should not be auto-expired
        // Only card registrations use payment expiry logic
        log.debug("⏭️ [VnpayPaymentExpiryJob] Invoice payment expiry is disabled - invoices only update status when payment succeeds");
        return;
        
        /* Original implementation (disabled):
        try {
            final OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(timeoutMinutes);
            
            // Tìm các invoice có VNPay payment đang pending quá thời gian timeout
            List<Invoice> expiredPayments = invoiceRepository.findExpiredVnpayPayments(threshold);

            if (expiredPayments.isEmpty()) {
                return;
            }

            log.info("⏰ [VnpayPaymentExpiryJob] Found {} VNPay payment(s) expired (older than {} minutes)", 
                    expiredPayments.size(), timeoutMinutes);

            int expiredCount = 0;
            for (Invoice invoice : expiredPayments) {
                // Chỉ expire nếu chưa được thanh toán
                if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
                    continue;
                }

                // Set response code to indicate timeout
                invoice.setVnpResponseCode("TIMEOUT");
                
                // Log the expiration
                log.info("❌ [VnpayPaymentExpiryJob] Expiring VNPay payment for invoice {} (initiated at: {}, elapsed: {} minutes)", 
                        invoice.getId(), 
                        invoice.getVnpayInitiatedAt(),
                        java.time.Duration.between(invoice.getVnpayInitiatedAt(), OffsetDateTime.now()).toMinutes());

                invoiceRepository.save(invoice);
                expiredCount++;
            }

            if (expiredCount > 0) {
                log.info("✅ [VnpayPaymentExpiryJob] Expired {} VNPay payment(s) after {} minutes timeout", 
                        expiredCount, timeoutMinutes);
            }

        } catch (Exception e) {
            log.error("❌ [VnpayPaymentExpiryJob] Error expiring pending VNPay payments", e);
        }
        */
    }
}
