package com.QhomeBase.baseservice.dto.residentview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResidentViewResidentDto {
    private UUID residentId;
    private String fullName;
    private String phone;
    private String email;
    private String nationalId;
    private LocalDate dob;
    private String relation; // From HouseholdMember
    private String role; // From UnitParty if existed? Or HouseholdKind?
    // Wait, requirement says: | Họ tên | SĐT | Email | Quan hệ | Vai trò |
    // "Quan hệ" is HouseholdMember.relation
    // "Vai trò" is probably HouseholdKind (Owner/Tenant) or UnitParty role?
    // Let's assume HouseholdKind or similar.
    private Boolean isPrimary;
    private String status;
}
