package com.QhomeBase.baseservice.dto.residentview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentExportDto {
    private Integer year;
    private String buildingCode;
    private String unitCode;
    private String fullName;
    private String phone;
    private String email;
    private String nationalId;
    private LocalDate dob;
    private String status;
    private String partyType; // HouseholdKind
    // private String role; // What is role vs party type? Maybe just same?
    private String relation;
    private Boolean isPrimary;
    private LocalDate startDate; // Household start date
    private LocalDate joinedAt; // Member join date
}
