package com.QhomeBase.servicescardservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingClient {

    private final RestTemplate restTemplate;

    @Value("${finance.billing.base-url:http://localhost:8085/api}")
    private String billingBaseUrl;

    public void recordVehicleRegistrationPayment(UUID registrationId,
                                                 UUID userId,
                                                 UUID unitId,
                                                 String vehicleType,
                                                 String licensePlate,
                                                 String requestType,
                                                 String note,
                                                 BigDecimal amount,
                                                 OffsetDateTime paymentDate,
                                                 String transactionRef,
                                                 String transactionNo,
                                                 String bankCode,
                                                 String cardType,
                                                 String responseCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("registrationId", registrationId != null ? registrationId.toString() : null);
            payload.put("userId", userId != null ? userId.toString() : null);
            payload.put("unitId", unitId != null ? unitId.toString() : null);
            payload.put("vehicleType", vehicleType);
            payload.put("licensePlate", licensePlate);
            payload.put("requestType", requestType);
            payload.put("note", note);
            payload.put("amount", amount);
            payload.put("paymentDate", paymentDate != null ? paymentDate.toString() : null);
            payload.put("transactionRef", transactionRef);
            payload.put("transactionNo", transactionNo);
            payload.put("bankCode", bankCode);
            payload.put("cardType", cardType);
            payload.put("responseCode", responseCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String url = billingBaseUrl + "/invoices/vehicle-registration-payment";

            restTemplate.postForEntity(url, request, Void.class);
            log.info("✅ [BillingClient] Đã gửi yêu cầu ghi nhận hóa đơn thẻ xe cho registration {}", registrationId);
        } catch (Exception e) {
            log.error("❌ [BillingClient] Không thể gửi hóa đơn thẻ xe tới billing-service: {}", e.getMessage());
        }
    }

    public void recordElevatorCardPayment(UUID registrationId,
                                          UUID userId,
                                          UUID unitId,
                                          String fullName,
                                          String apartmentNumber,
                                          String buildingName,
                                          String requestType,
                                          String note,
                                          BigDecimal amount,
                                          OffsetDateTime paymentDate,
                                          String transactionRef,
                                          String transactionNo,
                                          String bankCode,
                                          String cardType,
                                          String responseCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("registrationId", registrationId != null ? registrationId.toString() : null);
            payload.put("userId", userId != null ? userId.toString() : null);
            payload.put("unitId", unitId != null ? unitId.toString() : null);
            payload.put("fullName", fullName);
            payload.put("apartmentNumber", apartmentNumber);
            payload.put("buildingName", buildingName);
            payload.put("requestType", requestType);
            payload.put("note", note);
            payload.put("amount", amount);
            payload.put("paymentDate", paymentDate != null ? paymentDate.toString() : null);
            payload.put("transactionRef", transactionRef);
            payload.put("transactionNo", transactionNo);
            payload.put("bankCode", bankCode);
            payload.put("cardType", cardType);
            payload.put("responseCode", responseCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String url = billingBaseUrl + "/invoices/elevator-card-payment";

            restTemplate.postForEntity(url, request, Void.class);
            log.info("✅ [BillingClient] Đã gửi yêu cầu ghi nhận hóa đơn thẻ thang máy {}", registrationId);
        } catch (Exception e) {
            log.error("❌ [BillingClient] Không thể gửi hóa đơn thẻ thang máy tới billing-service: {}", e.getMessage());
        }
    }

    public void recordResidentCardPayment(UUID registrationId,
                                          UUID userId,
                                          UUID unitId,
                                          String fullName,
                                          String apartmentNumber,
                                          String buildingName,
                                          String requestType,
                                          String note,
                                          BigDecimal amount,
                                          OffsetDateTime paymentDate,
                                          String transactionRef,
                                          String transactionNo,
                                          String bankCode,
                                          String cardType,
                                          String responseCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("registrationId", registrationId != null ? registrationId.toString() : null);
            payload.put("userId", userId != null ? userId.toString() : null);
            payload.put("unitId", unitId != null ? unitId.toString() : null);
            payload.put("fullName", fullName);
            payload.put("apartmentNumber", apartmentNumber);
            payload.put("buildingName", buildingName);
            payload.put("requestType", requestType);
            payload.put("note", note);
            payload.put("amount", amount);
            payload.put("paymentDate", paymentDate != null ? paymentDate.toString() : null);
            payload.put("transactionRef", transactionRef);
            payload.put("transactionNo", transactionNo);
            payload.put("bankCode", bankCode);
            payload.put("cardType", cardType);
            payload.put("responseCode", responseCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            String url = billingBaseUrl + "/invoices/resident-card-payment";

            restTemplate.postForEntity(url, request, Void.class);
            log.info("✅ [BillingClient] Đã gửi yêu cầu ghi nhận hóa đơn thẻ cư dân {}", registrationId);
        } catch (Exception e) {
            log.error("❌ [BillingClient] Không thể gửi hóa đơn thẻ cư dân tới billing-service: {}", e.getMessage());
        }
    }
}


