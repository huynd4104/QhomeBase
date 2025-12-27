package com.QhomeBase.assetmaintenanceservice.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupplierRequest {

    private String name;

    private String type;

    private String contactPerson;

    private String phone;

    private String email;

    private String address;

    private String taxId;

    private String website;

    private String notes;

    private Boolean isActive;
}










