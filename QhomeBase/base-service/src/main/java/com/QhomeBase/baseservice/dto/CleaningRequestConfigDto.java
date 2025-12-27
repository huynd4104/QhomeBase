package com.QhomeBase.baseservice.dto;

import java.time.Duration;

public record CleaningRequestConfigDto(
        long reminderThresholdSeconds,
        long resendCancelThresholdSeconds,
        long noResendCancelThresholdSeconds
) {
    public CleaningRequestConfigDto(Duration reminderThreshold, Duration resendCancelThreshold, Duration noResendCancelThreshold) {
        this(
                reminderThreshold.getSeconds(),
                resendCancelThreshold.getSeconds(),
                noResendCancelThreshold.getSeconds()
        );
    }
}

