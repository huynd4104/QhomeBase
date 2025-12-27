package com.QhomeBase.iamservice.dto;

public enum EmployeePermissionStatus {
    STANDARD("Standard", "Nhân viên thường", "Nhân viên có quyền theo role mặc định", "default"),
    ELEVATED("Elevated", "Quyền nâng cao", "Nhân viên có thêm quyền đặc biệt", "success"),
    RESTRICTED("Restricted", "Quyền hạn chế", "Nhân viên bị hạn chế một số quyền", "warning"),
    MIXED("Mixed", "Quyền hỗn hợp", "Nhân viên có cả quyền nâng cao và hạn chế", "info"),
    TEMPORARY("Temporary", "Quyền tạm thời", "Nhân viên có quyền tạm thời", "primary");

    private final String code;
    private final String displayName;
    private final String description;
    private final String badgeColor;

    EmployeePermissionStatus(String code, String displayName, String description, String badgeColor) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.badgeColor = badgeColor;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public static EmployeePermissionStatus determineStatus(int grantedOverrides, int deniedOverrides, boolean hasTemporaryPermissions) {
        if (hasTemporaryPermissions) {
            return TEMPORARY;
        }
        
        if (grantedOverrides > 0 && deniedOverrides > 0) {
            return MIXED;
        } else if (grantedOverrides > 0) {
            return ELEVATED;
        } else if (deniedOverrides > 0) {
            return RESTRICTED;
        } else {
            return STANDARD;
        }
    }
}








































