package com.QhomeBase.baseservice.dto;

import com.QhomeBase.baseservice.model.ReadingCycleStatus;

public record ReadingCycleUpdateReq(
        String description,
        ReadingCycleStatus status
) {}

