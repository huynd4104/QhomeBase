package com.QhomeBase.marketplaceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private UUID id;
    private UUID residentId;
    private UUID buildingId;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String categoryName;
    private String status;
    private String scope; // BUILDING, ALL, or BOTH
    private ContactInfoResponse contactInfo;
    private String location;
    private Long viewCount;
    private Long commentCount;
    private List<PostImageResponse> images;
    private String videoUrl; // URL to video in data-docs-service
    private ResidentInfoResponse author;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public String getPriceDisplay() {
        if (price == null) return "Thỏa thuận";
        return String.format("%,.0f đ", price.doubleValue());
    }
}

