package com.QhomeBase.customerinteractionservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcceptDenyRequest {
    @NotBlank(message = "Action is required")
    private String action; // "accept" or "deny"
    
    private BigDecimal fee; // Required when action is "accept"
    
    private LocalDate repairedDate; // Required when action is "accept"
    
    private String note;
}
