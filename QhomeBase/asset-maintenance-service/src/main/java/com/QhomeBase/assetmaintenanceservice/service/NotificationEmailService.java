package com.QhomeBase.assetmaintenanceservice.service;

import com.QhomeBase.assetmaintenanceservice.client.BaseServiceClient;
import com.QhomeBase.assetmaintenanceservice.config.NotificationProperties;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBooking;
import com.QhomeBase.assetmaintenanceservice.model.service.ServiceBookingItem;
import com.QhomeBase.assetmaintenanceservice.model.service.enums.ServiceBookingItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;
    private final BaseServiceClient baseServiceClient;

    public void sendBookingPaymentSuccess(ServiceBooking booking, String txnRef) {
        // Get user email
        String userEmail = null;
        if (booking.getUserId() != null) {
            userEmail = baseServiceClient.getUserEmail(booking.getUserId());
            if (!StringUtils.hasText(userEmail)) {
                log.warn("📧 [Email] Could not get email for userId: {}", booking.getUserId());
            }
        }

        // Collect recipients: user email + admin emails (if configured)
        Set<String> recipients = new HashSet<>();
        if (StringUtils.hasText(userEmail)) {
            recipients.add(userEmail);
        }
        addAllSafe(recipients, notificationProperties.getServiceBookingSuccessRecipients());
        addAllSafe(recipients, notificationProperties.getServiceBookingSuccessCc());

        if (CollectionUtils.isEmpty(recipients)) {
            log.info("📧 [Email] No recipients configured for booking payment success notifications");
            return;
        }

        String subject = "[QHome] Xác nhận thanh toán dịch vụ thành công - Mã đơn: " + booking.getId();
        String body = buildBody(booking, txnRef);

        sendEmail(recipients, subject, body);
    }

    private void addAllSafe(Set<String> target, Collection<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
    }

    private void sendEmail(Collection<String> recipients, String subject, String body) {
        for (String recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipient);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("📧 [Email] Sent payment email to {}", recipient);
            } catch (MailException ex) {
                log.error("❌ [Email] Failed to send email to {}: {}", recipient, ex.getMessage());
            }
        }
    }

    private String buildBody(ServiceBooking booking, String txnRef) {
        var service = booking.getService();
        String bookingDate = booking.getBookingDate() != null ? booking.getBookingDate().format(DATE_FORMATTER) : "—";
        String startTime = booking.getStartTime() != null ? booking.getStartTime().format(TIME_FORMATTER) : "—";
        String endTime = booking.getEndTime() != null ? booking.getEndTime().format(TIME_FORMATTER) : "—";
        String amount = booking.getTotalAmount() != null ? formatAmount(booking.getTotalAmount()) : "0";
        String bookingId = booking.getId() != null ? booking.getId().toString() : "N/A";

        // Build booking items details
        String itemsDetails = buildBookingItemsDetails(booking.getBookingItems());

        return """
                Xin chào,

                Hệ thống đã ghi nhận thanh toán thành công cho đơn đặt dịch vụ của bạn.

                ════════════════════════════════════════════════════════
                THÔNG TIN ĐƠN ĐẶT DỊCH VỤ
                ════════════════════════════════════════════════════════

                📋 Mã hóa đơn: %s
                🏢 Dịch vụ: %s
                📅 Ngày sử dụng: %s
                ⏰ Khung giờ: %s - %s
                👥 Số người: %s
                💰 Tổng tiền: %s VND
                💳 Mã giao dịch VNPAY: %s

                ════════════════════════════════════════════════════════
                CHI TIẾT ĐƠN HÀNG
                ════════════════════════════════════════════════════════

                %s

                ════════════════════════════════════════════════════════

                Cảm ơn bạn đã sử dụng dịch vụ của QHome!

                Trân trọng,
                QHome Resident
                """.formatted(
                bookingId,
                service != null ? service.getName() : "Dịch vụ",
                bookingDate,
                startTime,
                endTime,
                booking.getNumberOfPeople() != null ? booking.getNumberOfPeople().toString() : "—",
                amount,
                txnRef != null ? txnRef : "N/A",
                itemsDetails
        );
    }

    private String buildBookingItemsDetails(List<ServiceBookingItem> items) {
        if (items == null || items.isEmpty()) {
            return "Không có chi tiết đơn hàng.";
        }

        List<String> itemLines = new ArrayList<>();
        List<ServiceBookingItem> tickets = new ArrayList<>();
        List<ServiceBookingItem> options = new ArrayList<>();
        List<ServiceBookingItem> combos = new ArrayList<>();

        // Group items by type
        for (ServiceBookingItem item : items) {
            if (item.getItemType() == ServiceBookingItemType.TICKET) {
                tickets.add(item);
            } else if (item.getItemType() == ServiceBookingItemType.OPTION) {
                options.add(item);
            } else if (item.getItemType() == ServiceBookingItemType.COMBO) {
                combos.add(item);
            }
        }

        // Build tickets section
        if (!tickets.isEmpty()) {
            itemLines.add("🎫 VÉ (TICKETS):");
            for (ServiceBookingItem ticket : tickets) {
                String quantity = ticket.getQuantity() != null ? ticket.getQuantity().toString() : "1";
                String unitPrice = formatAmount(ticket.getUnitPrice());
                String totalPrice = formatAmount(ticket.getTotalPrice());
                itemLines.add(String.format("   • %s x%s - Đơn giá: %s VND - Thành tiền: %s VND",
                        ticket.getItemName(), quantity, unitPrice, totalPrice));
            }
            itemLines.add("");
        }

        // Build options section
        if (!options.isEmpty()) {
            itemLines.add("⚙️ TÙY CHỌN (OPTIONS):");
            for (ServiceBookingItem option : options) {
                String quantity = option.getQuantity() != null ? option.getQuantity().toString() : "1";
                String unitPrice = formatAmount(option.getUnitPrice());
                String totalPrice = formatAmount(option.getTotalPrice());
                itemLines.add(String.format("   • %s x%s - Đơn giá: %s VND - Thành tiền: %s VND",
                        option.getItemName(), quantity, unitPrice, totalPrice));
            }
            itemLines.add("");
        }

        // Build combos section
        if (!combos.isEmpty()) {
            itemLines.add("📦 COMBO:");
            for (ServiceBookingItem combo : combos) {
                String quantity = combo.getQuantity() != null ? combo.getQuantity().toString() : "1";
                String unitPrice = formatAmount(combo.getUnitPrice());
                String totalPrice = formatAmount(combo.getTotalPrice());
                itemLines.add(String.format("   • %s x%s - Đơn giá: %s VND - Thành tiền: %s VND",
                        combo.getItemName(), quantity, unitPrice, totalPrice));
            }
            itemLines.add("");
        }

        return String.join("\n", itemLines);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        // Format with thousand separators
        return String.format("%,.0f", amount.doubleValue());
    }
}

