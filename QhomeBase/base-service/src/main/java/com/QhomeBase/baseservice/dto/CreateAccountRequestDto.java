package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public record CreateAccountRequestDto(
        UUID residentId,

        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username can only contain letters, numbers, underscore, and hyphen")
        String username,

        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        boolean autoGenerate,

        List<String> proofOfRelationImageUrls
) {
    public CreateAccountRequestDto {
        if (!autoGenerate) {
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Username is required when autoGenerate is false");
            }
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("Password is required when autoGenerate is false");
            }
        }

        proofOfRelationImageUrls = proofOfRelationImageUrls == null
                ? List.of()
                : proofOfRelationImageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .limit(6)
                .collect(Collectors.toList());

        if (proofOfRelationImageUrls.size() > 6) {
            throw new IllegalArgumentException("Only up to 6 proof images are allowed");
        }
    }
}

