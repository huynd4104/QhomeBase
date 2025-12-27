package com.QhomeBase.datadocsservice.dto;

import java.time.LocalDate;

public record CancelContractRequest(
        LocalDate scheduledDate // Ngày được chọn sẽ lưu vào inspectionDate thay vì scheduledDate (nếu null thì dùng ngày hiện tại)
) {
}
