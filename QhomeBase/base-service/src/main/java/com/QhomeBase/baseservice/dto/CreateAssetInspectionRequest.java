package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAssetInspectionRequest(
        UUID contractId,
        UUID unitId,
        LocalDate inspectionDate,
        LocalDate scheduledDate, // Ngày đã hẹn để nhân viên tới kiểm tra (nếu null thì dùng endDate của contract)
        String inspectorName,
        UUID inspectorId
) {
}

