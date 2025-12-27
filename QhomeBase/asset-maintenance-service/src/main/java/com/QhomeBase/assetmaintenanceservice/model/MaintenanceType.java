package com.QhomeBase.assetmaintenanceservice.model;

public enum MaintenanceType {
    ROUTINE("Bảo trì định kỳ"),
    REPAIR("Sửa chữa"),
    INSPECTION("Kiểm tra"),
    EMERGENCY("Khẩn cấp"),
    UPGRADE("Nâng cấp");

    private final String description;

    MaintenanceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}










