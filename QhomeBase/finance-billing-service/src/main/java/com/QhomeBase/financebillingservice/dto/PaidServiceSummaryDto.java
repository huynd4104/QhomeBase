package com.QhomeBase.financebillingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO tổng hợp các dịch vụ/hóa đơn đã thanh toán trong tháng
 * Bao gồm: invoices (điện, nước, gửi xe) + maintenance service bookings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaidServiceSummaryDto {
    
    private UUID id;
    private String code; // Invoice code or booking code
    private String type; // "INVOICE" or "SERVICE_BOOKING"
    private String serviceCode;
    private String serviceName;
    private UUID unitId;
    private UUID residentId;
    private BigDecimal amount;
    private String paymentGateway;
    private OffsetDateTime paidAt;
    private Integer paymentMonth;
    private Integer paymentYear;
}
