package com.QhomeBase.assetmaintenanceservice.model;

public enum MaintenanceRecordStatus {
    SCHEDULED("Đã lên lịch"),
    ASSIGNED("Đã gán"),
    IN_PROGRESS("Đang thực hiện"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy"),
    FAILED("Thất bại");

    private final String description;

    MaintenanceRecordStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}










