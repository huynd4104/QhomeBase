package com.QhomeBase.iamservice.model.imports;

import com.QhomeBase.iamservice.model.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StaffImportRow {
    private int rowNumber;
    private String username;
    private String email;
    private String password;
    private List<UserRole> roles;
    private Boolean active;
    private String fullName;
    private String phone;
    private String nationalId;
    private String address;
}
