package com.QhomeBase.customerinteractionservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFeeRequest {
    @NotNull(message = "Fee is required")
    private BigDecimal fee;
}
