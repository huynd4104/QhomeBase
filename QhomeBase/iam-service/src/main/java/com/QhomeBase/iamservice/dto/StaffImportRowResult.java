package com.QhomeBase.iamservice.dto;

import java.util.List;
import java.util.UUID;

public record StaffImportRowResult(
        int rowNumber,
        String username,
        String email,
        List<String> roles,
        Boolean active,
        boolean success,
        UUID createdUserId,
        String message
) {
}











