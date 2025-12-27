package com.QhomeBase.baseservice.model;

public enum MeterReadingAssignmentStatus {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    OVERDUE("Overdue");

    private final String description;

    MeterReadingAssignmentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

