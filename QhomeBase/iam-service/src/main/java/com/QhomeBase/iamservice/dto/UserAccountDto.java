package com.QhomeBase.iamservice.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record UserAccountDto(
        UUID userId,
        String username,
        String email,
        List<String> roles,
        boolean active
) implements Serializable {
    private static final long serialVersionUID = 1L;
}