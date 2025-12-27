package com.QhomeBase.datadocsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractRenewalResponse {

    private UUID newContractId;
    private String contractNumber;
    private BigDecimal totalAmount;
    private String paymentUrl; // VNPay payment URL
    private String message;
}
