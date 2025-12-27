package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.model.News;
import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class NotificationPushService {

    private static final int FCM_MAX_TOKENS_PER_REQUEST = 500;

    @Nullable
    private final FirebaseMessaging firebaseMessaging;
    private final NotificationDeviceTokenService deviceTokenService;
    
    public NotificationPushService(@Nullable FirebaseMessaging firebaseMessaging, NotificationDeviceTokenService deviceTokenService) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceTokenService = deviceTokenService;
        if (firebaseMessaging == null) {
            log.warn("⚠️ FirebaseMessaging is null - Push notifications will be disabled. Please fix Firebase service account key.");
        }
    }
    
    private boolean isFirebaseEnabled() {
        return firebaseMessaging != null;
    }

    public void sendPushNotification(Notification notification) {
        if (!isFirebaseEnabled()) {
            log.warn("⚠️ Firebase not initialized. Push notification skipped for notification ID: {}", notification.getId());
            return;
        }
        Map<String, String> dataPayload = buildDataPayload(notification);
        sendMulticast(
                Optional.ofNullable(notification.getTitle()).orElse("Thông báo mới"),
                Optional.ofNullable(notification.getMessage()).orElse(""),
                dataPayload,
                notification.getScope(),
                notification.getTargetBuildingId(),
                notification.getTargetRole()
        );
    }

    public void sendPushNotificationToResident(UUID residentId, String title, String body, Map<String, String> dataPayload) {
        if (!isFirebaseEnabled()) {
            log.warn("⚠️ Firebase not initialized. Push notification skipped for residentId: {}", residentId);
            return;
        }
        if (residentId == null) {
            log.warn("⚠️ residentId is null, skip push notification");
            return;
        }
        
        List<String> tokens = deviceTokenService.resolveTokensForResident(residentId);
        if (tokens.isEmpty()) {
            log.info("ℹ️ No device tokens found for residentId: {}", residentId);
            return;
        }

        log.info("🔔 Sending FCM push to {} tokens for residentId: {}", tokens.size(), residentId);

        com.google.firebase.messaging.Notification firebaseNotification =
                com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build();

        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i += FCM_MAX_TOKENS_PER_REQUEST) {
            int end = Math.min(i + FCM_MAX_TOKENS_PER_REQUEST, tokens.size());
            List<String> batch = tokens.subList(i, end);

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setChannelId("qhome_resident_channel")
                            .setSound("default")
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(firebaseNotification)
                    .setAndroidConfig(androidConfig)
                    .putAllData(dataPayload != null ? dataPayload : new HashMap<>())
                    .addAllTokens(batch)
                    .build();

            try {
                var response = firebaseMessaging.sendEachForMulticast(message);
                log.info("✅ FCM send result: success={}, failure={} for batch {} (residentId: {})",
                        response.getSuccessCount(), response.getFailureCount(), (i / FCM_MAX_TOKENS_PER_REQUEST) + 1, residentId);

                List<SendResponse> sendResponses = response.getResponses();
                for (int j = 0; j < sendResponses.size(); j++) {
                    SendResponse sendResponse = sendResponses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        String errorCode = Optional.ofNullable(sendResponse.getException())
                                .map(ex -> ex.getMessagingErrorCode() != null
                                        ? ex.getMessagingErrorCode().name()
                                        : ex.getMessage())
                                .orElse("UNKNOWN");

                        log.warn("⚠️ Failed to send token {} due to {} (residentId: {})",
                                batch.get(j), errorCode, residentId);

                        if ("UNREGISTERED".equalsIgnoreCase(errorCode)
                                || "INVALID_ARGUMENT".equalsIgnoreCase(errorCode)) {
                            invalidTokens.add(batch.get(j));
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("❌ Error sending FCM notification batch for residentId: {}", residentId, e);
            }
        }

        if (!invalidTokens.isEmpty()) {
            deviceTokenService.markTokensAsInvalid(invalidTokens);
        }
    }

    private Map<String, String> buildDataPayload(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", notification.getId().toString());
        data.put("type", notification.getType() != null ? notification.getType().name() : "SYSTEM");
        Optional.ofNullable(notification.getReferenceId())
                .map(UUID::toString)
                .ifPresent(id -> data.put("referenceId", id));
        Optional.ofNullable(notification.getReferenceType())
                .ifPresent(type -> data.put("referenceType", type));

        Optional.ofNullable(notification.getScope())
                .ifPresent(scope -> data.put("scope", scope.name()));

        Optional.ofNullable(notification.getTargetBuildingId())
                .map(UUID::toString)
                .ifPresent(id -> data.put("targetBuildingId", id));

        Optional.ofNullable(notification.getTargetRole())
                .ifPresent(role -> data.put("targetRole", role));

        return data;
    }

    public void sendNewsCreatedPush(News news) {
        if (!isFirebaseEnabled()) {
            log.warn("⚠️ Firebase not initialized. Push notification skipped for news ID: {}", news != null ? news.getId() : "null");
            return;
        }
        if (news == null || !news.isActive()) {
            return;
        }
        sendNewsNotification(news, "NEWS_CREATED");
    }

    public void sendNewsUpdatedPush(News news) {
        if (!isFirebaseEnabled()) {
            log.warn("⚠️ Firebase not initialized. Push notification skipped for news ID: {}", news != null ? news.getId() : "null");
            return;
        }
        if (news == null || !news.isActive()) {
            return;
        }
        sendNewsNotification(news, "NEWS_UPDATED");
    }

    private void sendNewsNotification(News news, String eventType) {
        NotificationScope scope = news.getScope() != null ? news.getScope() : NotificationScope.EXTERNAL;
        Map<String, String> data = new HashMap<>();
        data.put("type", eventType);
        data.put("newsId", news.getId().toString());
        Optional.ofNullable(news.getTargetBuildingId())
                .map(UUID::toString)
                .ifPresent(id -> data.put("targetBuildingId", id));
        Optional.ofNullable(news.getSummary()).ifPresent(summary -> data.put("summary", summary));
        Optional.ofNullable(news.getCoverImageUrl()).ifPresent(url -> data.put("coverImageUrl", url));

        String title = news.getTitle();
        String body = Optional.ofNullable(news.getSummary()).orElse("Có tin tức mới dành cho bạn");

        sendMulticast(
                title,
                body,
                data,
                scope,
                news.getTargetBuildingId(),
                news.getTargetRole()
        );
    }

    private void sendMulticast(String title,
                               String body,
                               Map<String, String> dataPayload,
                               NotificationScope scope,
                               UUID targetBuildingId,
                               String targetRole) {
        if (!isFirebaseEnabled()) {
            log.warn("⚠️ Firebase not initialized. Cannot send push notification.");
            return;
        }
        
        List<String> tokens = deviceTokenService.resolveTokens(scope, targetBuildingId, targetRole);
        if (tokens.isEmpty()) {
            log.info("ℹ️ No device tokens found for push scope={} building={}", scope, targetBuildingId);
            return;
        }

        log.info("🔔 Sending FCM push to {} tokens (scope={}, building={}, role={})",
                tokens.size(), scope, targetBuildingId, targetRole);

        com.google.firebase.messaging.Notification firebaseNotification =
                com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build();

        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i += FCM_MAX_TOKENS_PER_REQUEST) {
            int end = Math.min(i + FCM_MAX_TOKENS_PER_REQUEST, tokens.size());
            List<String> batch = tokens.subList(i, end);

            // Android config với priority HIGH để hiển thị khi app ở background
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setChannelId("qhome_resident_channel")
                            .setSound("default")
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(firebaseNotification)
                    .setAndroidConfig(androidConfig)
                    .putAllData(dataPayload)
                    .addAllTokens(batch)
                    .build();

            try {
                var response = firebaseMessaging.sendEachForMulticast(message);
                log.info("✅ FCM send result: success={}, failure={} for batch {}",
                        response.getSuccessCount(), response.getFailureCount(), (i / FCM_MAX_TOKENS_PER_REQUEST) + 1);

                List<SendResponse> sendResponses = response.getResponses();
                for (int j = 0; j < sendResponses.size(); j++) {
                    SendResponse sendResponse = sendResponses.get(j);
                    if (!sendResponse.isSuccessful()) {
                        String errorCode = Optional.ofNullable(sendResponse.getException())
                                .map(ex -> ex.getMessagingErrorCode() != null
                                        ? ex.getMessagingErrorCode().name()
                                        : ex.getMessage())
                                .orElse("UNKNOWN");

                        log.warn("⚠️ Failed to send token {} due to {}",
                                batch.get(j), errorCode);

                        if ("UNREGISTERED".equalsIgnoreCase(errorCode)
                                || "INVALID_ARGUMENT".equalsIgnoreCase(errorCode)) {
                            invalidTokens.add(batch.get(j));
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("❌ Error sending FCM notification batch", e);
            }
        }

        if (!invalidTokens.isEmpty()) {
            deviceTokenService.markTokensAsInvalid(invalidTokens);
        }
    }
}

