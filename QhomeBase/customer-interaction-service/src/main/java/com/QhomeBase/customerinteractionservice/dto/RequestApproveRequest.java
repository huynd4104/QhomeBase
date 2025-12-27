package com.QhomeBase.customerinteractionservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestApproveRequest(
        @NotBlank(message = "Content is required")
        String content,
        String note
) {
}
