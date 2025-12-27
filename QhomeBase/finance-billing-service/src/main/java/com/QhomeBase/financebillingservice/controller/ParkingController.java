package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.ProRataInvoiceRequest;
import com.QhomeBase.financebillingservice.dto.ProRataInvoiceResponse;
import com.QhomeBase.financebillingservice.service.ParkingBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingController {
    
    private final ParkingBillingService parkingBillingService;
    
    @PostMapping("/invoices/generate-prorata")
    public ResponseEntity<ProRataInvoiceResponse> generateProRataInvoice(
            @RequestBody ProRataInvoiceRequest request) {
        
        ProRataInvoiceResponse response = parkingBillingService.createProRataInvoice(request);
        
        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Parking Billing Service is running");
    }
}

