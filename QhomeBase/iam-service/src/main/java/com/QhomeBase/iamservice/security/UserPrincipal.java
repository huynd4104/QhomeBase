package com.QhomeBase.iamservice.security;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID uid,
        String username,
        UUID tenant,
        List<String> roles,
        List<String> perms,
        String token
) {
    public UserPrincipal {
        if (roles == null) {
            roles = List.of();
        }
        if (perms == null) {
            perms = List.of();
        }
    }
}
