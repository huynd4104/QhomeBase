package com.QhomeBase.baseservice.model;

/**
 * Loại thiết bị bàn giao trong căn hộ - theo danh mục hợp đồng
 */
public enum AssetType {
    // ========== Thiết bị nhà Tắm và Vệ sinh ==========
    TOILET, // Bồn cầu
    BATHROOM_SINK, // Chậu rửa nhà tắm
    WATER_HEATER, // Bình nóng lạnh
    SHOWER_SYSTEM, // Hệ sen vòi nhà tắm
    BATHROOM_FAUCET, // Vòi chậu rửa
    BATHROOM_LIGHT, // Đèn nhà tắm
    BATHROOM_DOOR, // Cửa nhà tắm
    BATHROOM_ELECTRICAL, // Hệ thống điện nhà vệ sinh

    // ========== Thiết bị phòng khách ==========
    LIVING_ROOM_DOOR, // Cửa phòng khách
    LIVING_ROOM_LIGHT, // Đèn phòng khách
    AIR_CONDITIONER, // Điều hòa
    INTERNET_SYSTEM, // Hệ thống mạng Internet
    FAN, // Quạt
    LIVING_ROOM_ELECTRICAL, // Hệ thống điện phòng khách

    // ========== Thiết bị phòng ngủ ==========
    BEDROOM_ELECTRICAL, // Hệ thống điện phòng ngủ
    BEDROOM_AIR_CONDITIONER, // Điều hòa phòng ngủ
    BEDROOM_DOOR, // Cửa phòng ngủ
    BEDROOM_WINDOW, // Cửa sổ phòng ngủ

    // ========== Thiết bị nhà bếp ==========
    KITCHEN_LIGHT, // Hệ thống đèn nhà bếp
    KITCHEN_ELECTRICAL, // Hệ thống điện nhà bếp
    ELECTRIC_STOVE, // Bếp điện
    KITCHEN_DOOR, // Cửa bếp và logia

    // ========== Thiết bị hành lang ==========
    HALLWAY_LIGHT, // Hệ thống đèn hành lang
    HALLWAY_ELECTRICAL, // Hệ thống điện hành lang

    // ========== Khác ==========
    OTHER // Khác
}
