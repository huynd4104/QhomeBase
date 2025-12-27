package com.QhomeBase.customerinteractionservice.model;

public enum NotificationType {
    INFO,           // Thông tin
    REQUEST,        // Yêu cầu
    BILL,           // Hóa Đơn
    CONTRACT,       // Hợp đồng
    ELECTRICITY,    // Tiền điện
    WATER,          // Tiền nước
    SYSTEM,         // Hệ thống
    // Backward compatibility - giữ các types cũ để không break existing data
    NEWS,           // Deprecated - dùng INFO thay thế
    METER_READING,  // Deprecated - dùng ELECTRICITY hoặc WATER thay thế
    CARD_APPROVED,
    CARD_REJECTED,
    CARD_FEE_REMINDER
}
