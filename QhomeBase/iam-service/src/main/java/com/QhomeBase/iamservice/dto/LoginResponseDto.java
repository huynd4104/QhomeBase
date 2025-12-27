package com.QhomeBase.iamservice.dto;

import java.time.Instant;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        Long expiresIn,
        Instant expiresAt,
        UserInfoDto userInfo
) {}