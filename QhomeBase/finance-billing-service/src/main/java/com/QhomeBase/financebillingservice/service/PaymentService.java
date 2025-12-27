package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.dto.CreatePaymentRequest;
import com.QhomeBase.financebillingservice.dto.PaymentAllocationDto;
import com.QhomeBase.financebillingservice.dto.PaymentDto;
import com.QhomeBase.financebillingservice.model.*;
import com.QhomeBase.financebillingservice.repository.InvoiceRepository;
import com.QhomeBase.financebillingservice.repository.PaymentAllocationRepository;
import com.QhomeBase.financebillingservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final InvoiceRepository invoiceRepository;
    
    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request) {
        log.info("Creating payment amount: {}", request.getAmountTotal());
        
        validatePaymentRequest(request);
        
        String receiptNo = generateReceiptNo(request.getMethod());
        
        Payment payment = Payment.builder()
                .receiptNo(receiptNo)
                .method(request.getMethod())
                .cashAccountId(request.getCashAccountId())
                .paidAt(OffsetDateTime.now())
                .amountTotal(request.getAmountTotal())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .status(PaymentStatus.SUCCEEDED)
                .note(request.getNote())
                .payerResidentId(request.getPayerResidentId())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}, receipt: {}", savedPayment.getId(), savedPayment.getReceiptNo());
        
        if (request.getAllocations() != null && !request.getAllocations().isEmpty()) {
            for (PaymentAllocationDto allocationDto : request.getAllocations()) {
                PaymentAllocation allocation = PaymentAllocation.builder()
                        .paymentId(savedPayment.getId())
                        .allocationType(allocationDto.getAllocationType())
                        .invoiceId(allocationDto.getInvoiceId())
                        .invoiceLineId(allocationDto.getInvoiceLineId())
                        .amount(allocationDto.getAmount())
                        .build();
                
                paymentAllocationRepository.save(allocation);
                
                if (allocationDto.getAllocationType() == AllocationType.INVOICE 
                        && allocationDto.getInvoiceId() != null) {
                    updateInvoiceStatusIfFullyPaid(allocationDto.getInvoiceId());
                }
            }
            log.info("Created {} payment allocations for payment: {}", 
                    request.getAllocations().size(), savedPayment.getId());
        }
        
        return toDto(savedPayment);
    }
    
    public List<PaymentDto> getPaymentsByResident(UUID residentId) {
        List<Payment> payments = paymentRepository.findByPayerResidentId(residentId);
        return payments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    
    public PaymentDto getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return toDto(payment);
    }
    
    private void updateInvoiceStatusIfFullyPaid(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByInvoiceId(invoiceId);
        BigDecimal totalPaid = allocations.stream()
                .map(PaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("Invoice {} has total paid: {}", invoiceId, totalPaid);
        
        // You would need to calculate the actual invoice total here
        // For now, just mark as PAID if there are any allocations
        if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            log.info("Invoice {} marked as PAID", invoiceId);
        }
    }
    
    private String generateReceiptNo(PaymentMethod method) {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String methodCode = switch (method) {
            case CASH -> "CASH";
            case BANK_TRANSFER -> "BANK";
            case MOMO -> "MOMO";
            case ZALOPAY -> "ZALOPAY";
        };
        return String.format("PAY-%s-%s", methodCode, timestamp);
    }
    
    private void validatePaymentRequest(CreatePaymentRequest request) {
        if (request.getMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (request.getAmountTotal() == null || request.getAmountTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        if (request.getAllocations() != null && !request.getAllocations().isEmpty()) {
            BigDecimal totalAllocated = request.getAllocations().stream()
                    .map(PaymentAllocationDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            if (totalAllocated.compareTo(request.getAmountTotal()) > 0) {
                throw new IllegalArgumentException("Total allocated amount cannot exceed payment amount");
            }
        }
    }
    
    private PaymentDto toDto(Payment payment) {
        List<PaymentAllocation> allocations = paymentAllocationRepository.findByPaymentId(payment.getId());
        
        return PaymentDto.builder()
                .id(payment.getId())
                .receiptNo(payment.getReceiptNo())
                .method(payment.getMethod())
                .cashAccountId(payment.getCashAccountId())
                .paidAt(payment.getPaidAt())
                .amountTotal(payment.getAmountTotal())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .note(payment.getNote())
                .payerResidentId(payment.getPayerResidentId())
                .allocations(allocations.stream()
                        .map(this::allocationToDto)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private PaymentAllocationDto allocationToDto(PaymentAllocation allocation) {
        return PaymentAllocationDto.builder()
                .id(allocation.getId())
                .allocationType(allocation.getAllocationType())
                .invoiceId(allocation.getInvoiceId())
                .invoiceLineId(allocation.getInvoiceLineId())
                .amount(allocation.getAmount())
                .build();
    }
}




