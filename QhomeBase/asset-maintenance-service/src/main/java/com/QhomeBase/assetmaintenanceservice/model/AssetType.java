package com.QhomeBase.assetmaintenanceservice.model;

public enum AssetType {
    ELEVATOR("Thang máy"),
    GENERATOR("Máy phát điện"),
    AIR_CONDITIONER("Điều hòa"),
    WATER_PUMP("Máy bơm nước"),
    SECURITY_SYSTEM("Hệ thống an ninh"),
    FIRE_SAFETY("Hệ thống phòng cháy chữa cháy"),
    CCTV("Camera giám sát"),
    INTERCOM("Hệ thống liên lạc nội bộ"),
    GATE_BARRIER("Rào chắn cổng"),
    SWIMMING_POOL("Hồ bơi"),
    GYM_EQUIPMENT("Thiết bị gym"),
    PLAYGROUND("Sân chơi"),
    PARKING_SYSTEM("Hệ thống đỗ xe"),
    GARDEN_IRRIGATION("Hệ thống tưới vườn"),
    LIGHTING_SYSTEM("Hệ thống chiếu sáng"),
    WIFI_SYSTEM("Hệ thống WiFi"),
    SOLAR_PANEL("Tấm năng lượng mặt trời"),
    WASTE_MANAGEMENT("Hệ thống quản lý rác"),
    MAINTENANCE_TOOL("Công cụ bảo trì"),
    OTHER("Khác");

    private final String description;

    AssetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}



