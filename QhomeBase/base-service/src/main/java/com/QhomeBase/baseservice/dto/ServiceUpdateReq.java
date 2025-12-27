package com.QhomeBase.baseservice.dto;

public record ServiceUpdateReq(
        String name,
        String nameEn,
        String unitLabel,
        Boolean billable,
        Boolean active,
        String description,
        Integer displayOrder
) {}

