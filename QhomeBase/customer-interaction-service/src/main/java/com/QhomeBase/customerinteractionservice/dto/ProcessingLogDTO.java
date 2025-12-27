package com.QhomeBase.customerinteractionservice.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingLogDTO {
    UUID id;
    UUID recordId;
    String content;
    String requestStatus;
    String staffInChargeName;
    String staffInChargeEmail;
    String createdAt;
}
