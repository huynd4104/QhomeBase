package com.QhomeBase.customerinteractionservice.dto.news;

import com.QhomeBase.customerinteractionservice.model.NewsStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsFilterRequest {

    private NewsStatus status;
    private UUID buildingId;
    private String keyword;
    private Instant publishFrom;
    private Instant publishTo;
    private Boolean expired;
    private Boolean activeOnly;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDir = "desc";
}

