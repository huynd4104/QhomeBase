package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminServiceRequestActionDto(
        @NotBlank(message = "Note is required")
        String note
) {
}

