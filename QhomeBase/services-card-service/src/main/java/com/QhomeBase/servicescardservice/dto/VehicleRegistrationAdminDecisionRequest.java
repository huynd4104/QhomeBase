package com.QhomeBase.servicescardservice.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistrationAdminDecisionRequest {

    private String decision; // Optional, not used for vehicle registration

    @Size(max = 2000, message = "Ghi chú không được vượt quá 2000 ký tự")
    private String note;

    @Size(max = 2000, message = "Thông điệp gửi cư dân không được vượt quá 2000 ký tự")
    private String issueMessage;

    private OffsetDateTime issueTime;
}

