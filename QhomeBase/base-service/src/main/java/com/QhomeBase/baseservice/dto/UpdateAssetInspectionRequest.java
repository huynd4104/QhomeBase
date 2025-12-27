package com.QhomeBase.baseservice.dto;

import java.time.LocalDate;

public record UpdateAssetInspectionRequest(
        LocalDate scheduledDate // Cho phép cập nhật scheduledDate nhiều lần
) {}
