package com.QhomeBase.assetmaintenanceservice.model;

public enum AssetStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Ngừng hoạt động"),
    MAINTENANCE("Đang bảo trì"),
    REPAIRING("Đang sửa chữa"),
    REPLACED("Đã thay thế"),
    DECOMMISSIONED("Đã ngừng sử dụng");

    private final String description;

    AssetStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}










