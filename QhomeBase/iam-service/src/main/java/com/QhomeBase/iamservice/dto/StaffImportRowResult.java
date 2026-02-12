package com.QhomeBase.iamservice.dto;

import java.util.List;
import java.util.UUID;

public record StaffImportRowResult(
                int rowNumber,
                String username,
                String email,
                String password,
                List<String> roles,
                Boolean active,
                String fullName,
                String phone,
                String nationalId,
                String address,
                boolean success,
                UUID createdUserId,
                String message) {
}
