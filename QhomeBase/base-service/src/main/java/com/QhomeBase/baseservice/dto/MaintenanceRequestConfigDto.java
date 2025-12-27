package com.QhomeBase.baseservice.dto;

import java.time.Duration;

public record MaintenanceRequestConfigDto(
        Duration reminderThreshold,
        Duration callThreshold,
        String adminPhone
) {
    public long reminderThresholdMinutes() {
        return reminderThreshold.toMinutes();
    }

    public long callThresholdMinutes() {
        return callThreshold.toMinutes();
    }
}

