package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.CreatePaymentRequest;
import com.QhomeBase.financebillingservice.dto.PaymentDto;
import com.QhomeBase.financebillingservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping
    public ResponseEntity<PaymentDto> createPayment(@RequestBody CreatePaymentRequest request) {
        PaymentDto payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }
    
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByResident(@PathVariable UUID residentId) {
        List<PaymentDto> payments = paymentService.getPaymentsByResident(residentId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable UUID id) {
        PaymentDto payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(payment);
    }
}




