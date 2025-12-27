package com.QhomeBase.baseservice.dto;

public record AdminCommonAreaMaintenanceResponseDto(
        // adminResponse là optional - admin có thể ghi chú khi accept/deny
        String adminResponse
) {
}
