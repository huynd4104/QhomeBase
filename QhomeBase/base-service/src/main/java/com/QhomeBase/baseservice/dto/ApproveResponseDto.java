package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveResponseDto(
        @NotBlank(message = "Payment method is required")
        String paymentMethod // "DIRECT" or "VNPAY"
) {}

