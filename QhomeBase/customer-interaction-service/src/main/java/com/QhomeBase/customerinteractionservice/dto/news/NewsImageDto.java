package com.QhomeBase.customerinteractionservice.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsImageDto {

    private UUID id;
    private UUID newsId;
    private String url;
    private String caption;
    private Integer sortOrder;
    private Long fileSize;
    private String contentType;
}

