package com.QhomeBase.baseservice.dto;

import java.util.Map;

public record ServiceRequestOverviewDtos(
        long total,
        Map<String, Long> statusCounts
) 
{
    public ServiceRequestOverviewDtos(long total, Map<String, Long> statusCounts) {
        this.total = total;
        this.statusCounts = statusCounts;
    }
}
