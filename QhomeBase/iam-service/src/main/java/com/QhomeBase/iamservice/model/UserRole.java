package com.QhomeBase.iamservice.model;

public enum UserRole {
    ADMIN("Administrator"),
    ACCOUNTANT("Accountant"),
    TECHNICIAN("Technician"),
    SUPPORTER("Supporter"),
    RESIDENT("Resident"),
    UNIT_OWNER("Unit Owner");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    public boolean isAccountant() {
        return this == ACCOUNTANT;
    }
    
    public boolean isTechnician() {
        return this == TECHNICIAN;
    }
    
    public boolean isSupporter() {
        return this == SUPPORTER;
    }
    
    public boolean isResident() {
        return this == RESIDENT;
    }
    
    public boolean isUnitOwner() {
        return this == UNIT_OWNER;
    }
    
    public String getRoleName() {
        return this.name().toLowerCase();
    }
}