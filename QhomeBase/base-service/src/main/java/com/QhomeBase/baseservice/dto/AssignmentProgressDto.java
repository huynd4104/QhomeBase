package com.QhomeBase.baseservice.dto;

import java.time.OffsetDateTime;

public record AssignmentProgressDto(
    int totalMeters,
    int readingsDone,
    int readingsRemain,
    double percent,
    boolean completed,
    OffsetDateTime completedAt
) {}
