package com.QhomeBase.baseservice.dto;

import java.util.List;
import java.util.UUID;

public record ResidentAccountDto(
        UUID userId,
        String username,
        String email,
        List<String> roles,
        boolean active
) {}


