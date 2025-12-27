package com.QhomeBase.iamservice.dto;

import java.util.List;

public record UserInfoDto(
        String userId,
        String username,
        String email,
        List<String> roles,
        List<String> permissions
) {}