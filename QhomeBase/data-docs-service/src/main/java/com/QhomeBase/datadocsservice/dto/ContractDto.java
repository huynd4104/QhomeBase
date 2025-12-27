package com.QhomeBase.datadocsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractDto {

    private UUID id;
    private UUID unitId;
    private String contractNumber;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate checkoutDate;
    private BigDecimal monthlyRent;
    private BigDecimal totalRent; // Calculated total rent amount for RENTAL contracts
    private BigDecimal purchasePrice;
    private String paymentMethod;
    private String paymentTerms;
    private LocalDate purchaseDate;
    private String notes;
    private String status;
    private UUID createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private OffsetDateTime renewalReminderSentAt;
    private OffsetDateTime renewalDeclinedAt;
    private String renewalStatus;
    private Integer reminderCount; // Calculated: 1, 2, or 3 based on days since first reminder
    private Boolean isFinalReminder; // true if reminderCount == 3
    private Boolean needsRenewal; // true if contract is within 1 month before expiration (28-32 days before endDate, same as reminder 1)
    private UUID renewedContractId; // ID of the new contract created when this contract is renewed successfully
    private List<ContractFileDto> files;
    
    // Permission fields for UI
    private Boolean isOwner; // true if current user is OWNER or TENANT of the unit
    private Boolean canRenew; // true if user can renew contract (isOwner && contract is renewable)
    private Boolean canCancel; // true if user can cancel contract (isOwner && contract is cancellable)
    private Boolean canExtend; // true if user can extend contract (isOwner && contract is extendable)
    private String permissionMessage; // Message to display if user doesn't have permission (e.g., "Bạn không phải chủ căn hộ nên không thể gia hạn hay hủy hợp đồng")
    private LocalDate inspectionDate; // Ngày kiểm tra tài sản (từ AssetInspection)
}

