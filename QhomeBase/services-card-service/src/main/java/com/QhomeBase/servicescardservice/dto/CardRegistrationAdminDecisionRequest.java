package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record CardRegistrationAdminDecisionRequest(
        @NotBlank(message = "decision is required")
        String decision,
        String note,
        String issueMessage,
        OffsetDateTime issueTime
) {
}

