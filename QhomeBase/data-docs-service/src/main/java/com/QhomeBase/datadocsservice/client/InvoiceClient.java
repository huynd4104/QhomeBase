package com.QhomeBase.datadocsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class InvoiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.finance-billing.base-url:http://localhost:8085}")
    private String financeBillingServiceBaseUrl;

    public UUID createContractRenewalInvoice(UUID contractId,
                                              UUID unitId,
                                              UUID residentId,
                                              String contractNumber,
                                              String unitCode,
                                              java.math.BigDecimal totalAmount,
                                              java.time.LocalDate startDate,
                                              java.time.LocalDate endDate) {
        try {
            String url = financeBillingServiceBaseUrl + "/api/invoices";

            Map<String, Object> request = new HashMap<>();
            request.put("dueDate", endDate.toString());
            request.put("currency", "VND");
            request.put("payerUnitId", unitId.toString());
            request.put("payerResidentId", residentId.toString());
            request.put("status", "PAID"); // Invoice is already paid via VNPay
            
            // Bill to info
            request.put("billToName", "Cư dân - " + unitCode);
            request.put("billToAddress", "");
            request.put("billToContact", "");

            // Invoice lines
            java.util.List<Map<String, Object>> lines = new java.util.ArrayList<>();
            Map<String, Object> line = new HashMap<>();
            line.put("serviceDate", startDate.toString());
            line.put("description", String.format("Gia hạn hợp đồng %s từ %s đến %s", 
                    contractNumber, startDate, endDate));
            line.put("quantity", java.math.BigDecimal.ONE);
            line.put("unit", "hợp đồng");
            line.put("unitPrice", totalAmount);
            line.put("taxRate", java.math.BigDecimal.ZERO);
            line.put("serviceCode", "CONTRACT_RENEWAL");
            line.put("externalRefType", "CONTRACT");
            line.put("externalRefId", contractId.toString());
            lines.add(line);
            request.put("lines", lines);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object invoiceIdObj = response.getBody().get("id");
                if (invoiceIdObj != null) {
                    UUID invoiceId = invoiceIdObj instanceof UUID 
                            ? (UUID) invoiceIdObj 
                            : UUID.fromString(invoiceIdObj.toString());
                    log.info("✅ [InvoiceClient] Created invoice {} for contract renewal {} with paidAt set to current time", invoiceId, contractId);
                    return invoiceId;
                }
            }
            log.warn("⚠️ [InvoiceClient] Failed to create invoice: response body is null or missing id");
            return null;
        } catch (Exception ex) {
            log.error("❌ [InvoiceClient] Error creating invoice for contract renewal {}", contractId, ex);
            return null;
        }
    }
    
    public void updateInvoicePaidAt(UUID invoiceId) {
        try {
            String url = financeBillingServiceBaseUrl + "/api/invoices/" + invoiceId + "/status";
            
            // Update invoice status to PAID and set paidAt
            Map<String, Object> request = new HashMap<>();
            request.put("status", "PAID");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(request, headers),
                    Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ [InvoiceClient] Updated invoice {} status to PAID (paidAt should be set automatically)", invoiceId);
            } else {
                log.warn("⚠️ [InvoiceClient] Failed to update invoice {} status: {}", invoiceId, response.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("❌ [InvoiceClient] Error updating invoice {} status", invoiceId, ex);
        }
    }
}
