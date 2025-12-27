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
public class NewsListResponse {

    private UUID id;
    private String title;
    private String summary;
    private String coverImageUrl;
    private NewsStatus status;
    private Instant publishAt;
    private Instant expireAt;
    private Long viewCount;
    private Boolean isRead;
    private Instant createdAt;
    private Instant updatedAt;
}

