package com.QhomeBase.assetmaintenanceservice.security;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        @NotNull UUID uid,
        String username,
        List<String> roles,
        List<String> perms,
        String token
) {}










