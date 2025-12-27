package com.QhomeBase.assetmaintenanceservice.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupplierRequest {

    @NotBlank(message = "Supplier name is required")
    private String name;

    @NotBlank(message = "Supplier type is required")
    @Builder.Default
    private String type = "SUPPLIER"; // Chỉ dùng cho nhà cung cấp thiết bị. Maintenance và warranty do nội bộ xử lý.

    private String contactPerson;

    private String phone;

    private String email;

    private String address;

    private String taxId;

    private String website;

    private String notes;

    @Builder.Default
    private Boolean isActive = true;
}

