package com.QhomeBase.baseservice.dto;

public record VnpayUrlResponseDto(
        String paymentUrl,
        String transactionRef
) {}

