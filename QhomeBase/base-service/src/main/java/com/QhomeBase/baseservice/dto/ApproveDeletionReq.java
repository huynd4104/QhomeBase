package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveDeletionReq(
        @NotBlank String note
) {}