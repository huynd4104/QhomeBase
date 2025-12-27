package com.QhomeBase.assetmaintenanceservice.model;

public enum SupplierType {
    SUPPLIER("Nhà cung cấp thiết bị");

    private final String description;

    SupplierType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

