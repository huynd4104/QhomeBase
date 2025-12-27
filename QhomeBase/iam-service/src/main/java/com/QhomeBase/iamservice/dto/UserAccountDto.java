package com.QhomeBase.iamservice.dto;

import java.util.List;
import java.util.UUID;

public record UserAccountDto(
        UUID userId,
        String username,
        String email,
        List<String> roles,
        boolean active
) {}

