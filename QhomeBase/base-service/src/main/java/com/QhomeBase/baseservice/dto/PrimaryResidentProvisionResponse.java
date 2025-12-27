package com.QhomeBase.baseservice.dto;

import java.util.UUID;

public record PrimaryResidentProvisionResponse(
        UUID residentId,
        UUID householdMemberId,
        ResidentAccountDto account
) {}


