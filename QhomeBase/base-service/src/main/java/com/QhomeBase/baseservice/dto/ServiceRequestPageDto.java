package com.QhomeBase.baseservice.dto;

import java.util.List;
import java.util.Map;

public record ServiceRequestPageDto<T>(
        List<T> requests,
        long total,
        Map<String, Long> statusCounts
) {
    public ServiceRequestPageDto(List<T> requests, long total, Map<String, Long> statusCounts) {
        this.requests = requests;
        this.total = total;
        this.statusCounts = statusCounts;
    }
}
