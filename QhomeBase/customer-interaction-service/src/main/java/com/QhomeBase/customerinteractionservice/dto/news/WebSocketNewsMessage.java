package com.QhomeBase.customerinteractionservice.dto.news;

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
public class WebSocketNewsMessage {

    private String type;
    private UUID newsId;
    private String title;
    private String summary;
    private String coverImageUrl;
    private Instant timestamp;
    private String deepLink;

    public static WebSocketNewsMessage created(UUID newsId, String title, String summary, String coverImageUrl) {
        return WebSocketNewsMessage.builder()
                .type("NEWS_CREATED")
                .newsId(newsId)
                .title(title)
                .summary(summary)
                .coverImageUrl(coverImageUrl)
                .timestamp(Instant.now())
                .deepLink("qhome://news/" + newsId)
                .build();
    }

    public static WebSocketNewsMessage updated(UUID newsId, String title, String summary, String coverImageUrl) {
        return WebSocketNewsMessage.builder()
                .type("NEWS_UPDATED")
                .newsId(newsId)
                .title(title)
                .summary(summary)
                .coverImageUrl(coverImageUrl)
                .timestamp(Instant.now())
                .deepLink("qhome://news/" + newsId)
                .build();
    }

    public static WebSocketNewsMessage deleted(UUID newsId) {
        return WebSocketNewsMessage.builder()
                .type("NEWS_DELETED")
                .newsId(newsId)
                .timestamp(Instant.now())
                .build();
    }
}

