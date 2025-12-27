package com.QhomeBase.iamservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {
    private UUID userId;
    private String fullName;
    private String phoneNumber;
    private String department;
    private String position;
    private boolean isActive;
}








































