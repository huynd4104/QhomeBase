package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.BuildingInvoiceSummaryDto;
import com.QhomeBase.financebillingservice.dto.InvoiceDto;
import com.QhomeBase.financebillingservice.service.BillingCycleInvoiceService;
import com.QhomeBase.financebillingservice.service.BillingCycleExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing-cycles/{cycleId}")
@RequiredArgsConstructor
@Slf4j
public class BillingCycleInvoiceController {

    private final BillingCycleInvoiceService billingCycleInvoiceService;
    private final BillingCycleExportService billingCycleExportService;

    @GetMapping("/buildings")
    public List<BuildingInvoiceSummaryDto> getBuildingSummary(
            @PathVariable UUID cycleId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month
    ) {
        return billingCycleInvoiceService.summarizeByCycle(cycleId, serviceCode, month);
    }

    @GetMapping("/buildings/{buildingId}/invoices")
    public List<InvoiceDto> getInvoicesByBuilding(
            @PathVariable UUID cycleId,
            @PathVariable UUID buildingId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month) {
        return billingCycleInvoiceService.getInvoicesByBuilding(cycleId, buildingId, serviceCode, month);
    }

    @GetMapping("/invoices")
    public List<InvoiceDto> getInvoicesByCycle(
            @PathVariable UUID cycleId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month) {
        return billingCycleInvoiceService.getInvoicesByCycle(cycleId, serviceCode, month);
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportBillingCycle(
            @PathVariable UUID cycleId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) UUID buildingId) {
        try {
            byte[] bytes = billingCycleExportService.exportBillingCycleToExcel(cycleId, serviceCode, month, buildingId);
            String filename = String.format("billing_cycle_%s_%s.xlsx", cycleId.toString().substring(0, 8), 
                    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes);
        } catch (Exception e) {
            log.error("Failed to export billing cycle", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

