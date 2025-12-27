package com.QhomeBase.customerinteractionservice.scheduler;

import com.QhomeBase.customerinteractionservice.model.News;
import com.QhomeBase.customerinteractionservice.model.NewsStatus;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.repository.NewsRepository;
import com.QhomeBase.customerinteractionservice.service.NewsNotificationService;
import com.QhomeBase.customerinteractionservice.service.NotificationPushService;
import com.QhomeBase.customerinteractionservice.dto.news.WebSocketNewsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler để tự động publish các news có status SCHEDULED
 * khi publish_at <= now()
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NewsPublishScheduler {

    private final NewsRepository newsRepository;
    private final NewsNotificationService notificationService;
    private final NotificationPushService notificationPushService;

    
    @Scheduled(cron = "0 * * * * ?") // Mỗi phút
    @Transactional
    public void publishScheduledNews() {
        Instant now = Instant.now();
        List<News> scheduledNews = newsRepository.findByStatusAndPublishAtLessThanEqual(
                NewsStatus.SCHEDULED, 
                now
        );

        if (scheduledNews.isEmpty()) {
            log.debug("No scheduled news to publish at {}", now);
            return;
        }

        log.info("Found {} scheduled news to publish", scheduledNews.size());

        for (News news : scheduledNews) {
            try {
                // Chuyển status từ SCHEDULED sang PUBLISHED
                news.setStatus(NewsStatus.PUBLISHED);
                news.setUpdatedAt(Instant.now());
                newsRepository.save(news);

                log.info("✅ Published scheduled news: id={}, title={}, publishAt={}", 
                        news.getId(), news.getTitle(), news.getPublishAt());

                // Gửi notification nếu news có scope EXTERNAL (cho cư dân)
                if (news.getScope() != null && 
                    news.getScope() == NotificationScope.EXTERNAL && 
                    shouldSendNotificationForNews(news)) {
                    
                    WebSocketNewsMessage wsMessage = WebSocketNewsMessage.created(
                            news.getId(),
                            news.getTitle(),
                            news.getSummary(),
                            news.getCoverImageUrl());
                    
                    notificationService.notifyNewsCreated(wsMessage);
                    notificationPushService.sendNewsCreatedPush(news);
                    
                    log.info("✅ Sent realtime and FCM push notification for auto-published news {}", news.getId());
                }
            } catch (Exception e) {
                log.error("❌ Error publishing scheduled news: id={}, title={}", 
                        news.getId(), news.getTitle(), e);
            }
        }
    }

    /**
     * Kiểm tra xem có nên gửi notification cho news không
     * Chỉ gửi cho news có status PUBLISHED, scope EXTERNAL, và publishAt <= now
     */
    private boolean shouldSendNotificationForNews(News news) {
        if (news.getStatus() != NewsStatus.PUBLISHED) {
            return false;
        }
        
        if (news.getScope() == null || 
            news.getScope() != NotificationScope.EXTERNAL) {
            return false;
        }
        
        Instant now = Instant.now();
        if (news.getPublishAt() != null && news.getPublishAt().isAfter(now)) {
            return false;
        }
        
        return true;
    }
}

