package com.QhomeBase.customerinteractionservice.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsStatsResponse {

    private Long viewCount;
    private Long residentViewCount;
    private Long userViewCount;
    private Long totalTargetedUsers;
    private Double readRate;
}

