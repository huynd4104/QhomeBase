package com.QhomeBase.customerinteractionservice.dto.news;

import com.QhomeBase.customerinteractionservice.model.NewsStatus;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewsRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String summary;

    @NotBlank(message = "Body content is required")
    private String bodyHtml;

    private String coverImageUrl;

    @NotNull(message = "Status is required")
    @Builder.Default
    private NewsStatus status = NewsStatus.DRAFT;

    private Instant publishAt;

    private Instant expireAt;

    @Builder.Default
    private Integer displayOrder = 0;

    @NotNull(message = "Scope is required")
    private NotificationScope scope;

    private String targetRole;

    private UUID targetBuildingId;

    private List<NewsImageDto> images;
}

