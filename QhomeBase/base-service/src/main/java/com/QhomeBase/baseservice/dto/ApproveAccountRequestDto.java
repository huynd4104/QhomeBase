package com.QhomeBase.baseservice.dto;

public record ApproveAccountRequestDto(
        boolean approve,
        String rejectionReason
) {}

