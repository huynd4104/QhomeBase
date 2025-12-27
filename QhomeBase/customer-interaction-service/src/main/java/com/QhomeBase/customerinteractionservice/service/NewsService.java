package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.client.BaseServiceClient;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdDto;
import com.QhomeBase.customerinteractionservice.client.dto.HouseholdMemberDto;
import com.QhomeBase.customerinteractionservice.client.dto.UnitDto;
import com.QhomeBase.customerinteractionservice.dto.news.*;
import com.QhomeBase.customerinteractionservice.model.*;
import com.QhomeBase.customerinteractionservice.repository.NewsRepository;
import com.QhomeBase.customerinteractionservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsNotificationService notificationService;
    private final NotificationPushService notificationPushService;
    private final BaseServiceClient baseServiceClient;

    public NewsManagementResponse createNews(CreateNewsRequest request, Authentication authentication) {
        var principal = (UserPrincipal) authentication.getPrincipal();
        UUID userId = principal.uid();

        validateNewsScope(request.getScope(), request.getTargetRole(), request.getTargetBuildingId());

        News news = News.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .bodyHtml(request.getBodyHtml())
                .coverImageUrl(request.getCoverImageUrl())
                .status(request.getStatus())
                .publishAt(request.getPublishAt())
                .expireAt(request.getExpireAt())
                .displayOrder(request.getDisplayOrder())
                .scope(request.getScope())
                .targetRole(request.getTargetRole())
                .targetBuildingId(request.getTargetBuildingId())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            for (NewsImageDto imgDto : request.getImages()) {
                NewsImage image = NewsImage.builder()
                        .url(imgDto.getUrl())
                        .caption(imgDto.getCaption())
                        .sortOrder(imgDto.getSortOrder())
                        .fileSize(imgDto.getFileSize())
                        .contentType(imgDto.getContentType())
                        .build();
                news.addImage(image);
            }
        }

        News savedNews = newsRepository.save(news);

        // Chỉ gửi realtime notification và FCM push khi:
        // 1. status = PUBLISHED
        // 2. scope = EXTERNAL (cho cư dân)
        // 3. publishAt <= now (không phải tương lai)
        if (shouldSendNotificationForNews(savedNews)) {
            WebSocketNewsMessage wsMessage = WebSocketNewsMessage.created(
                    savedNews.getId(),
                    savedNews.getTitle(),
                    savedNews.getSummary(),
                    savedNews.getCoverImageUrl());
            notificationService.notifyNewsCreated(wsMessage);
            notificationPushService.sendNewsCreatedPush(savedNews);
            log.info("✅ [NewsService] Sent realtime and FCM push notification for news {} (PUBLISHED, EXTERNAL, publishAt <= now)", savedNews.getId());
        } else {
            log.info("⏭️ [NewsService] Skipped sending notification for news {} (status={}, scope={}, publishAt={})", 
                    savedNews.getId(), savedNews.getStatus(), savedNews.getScope(), savedNews.getPublishAt());
        }

        return toManagementResponse(savedNews);
    }

    private NewsDetailResponse toDetailResponse(News news) {
        // Normalize coverImageUrl: convert empty string to null
        String coverImageUrl = news.getCoverImageUrl();
        if (coverImageUrl != null && coverImageUrl.trim().isEmpty()) {
            coverImageUrl = null;
        }
        
        return NewsDetailResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .bodyHtml(news.getBodyHtml())
                .coverImageUrl(coverImageUrl)
                .status(news.getStatus())
                .publishAt(news.getPublishAt())
                .expireAt(news.getExpireAt())
                .displayOrder(news.getDisplayOrder())
                .viewCount(news.getViewCount())
                .images(toImageDtos(news.getImages()))
                .createdBy(news.getCreatedBy())
                .createdAt(news.getCreatedAt())
                .updatedBy(news.getUpdatedBy())
                .updatedAt(news.getUpdatedAt())
                .build();
    }

    private List<NewsImageDto> toImageDtos(List<NewsImage> images) {
        if (images == null)
            return List.of();
        return images.stream()
                .map(img -> NewsImageDto.builder()
                        .id(img.getId())
                        .newsId(img.getNews().getId())
                        .url(img.getUrl())
                        .caption(img.getCaption())
                        .sortOrder(img.getSortOrder())
                        .fileSize(img.getFileSize())
                        .contentType(img.getContentType())
                        .build())
                .collect(Collectors.toList());
    }

    public NewsManagementResponse updateNews(UUID newsId, UpdateNewsRequest request, Authentication auth) {
        var principal = (UserPrincipal) auth.getPrincipal();
        UUID userId = principal.uid();

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));
        if (request.getTitle() != null) {
            news.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            news.setSummary(request.getSummary());
        }
        if (request.getBodyHtml() != null) {
            news.setBodyHtml(request.getBodyHtml());
        }
        if (request.getCoverImageUrl() != null) {
            news.setCoverImageUrl(request.getCoverImageUrl());
        }
        if (request.getStatus() != null) {
            news.setStatus(request.getStatus());
        }
        if (request.getPublishAt() != null) {
            news.setPublishAt(request.getPublishAt());
        }
        if (request.getExpireAt() != null) {
            news.setExpireAt(request.getExpireAt());
        }
        if (request.getDisplayOrder() != null) {
            news.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getScope() != null) {
            news.setScope(request.getScope());
            validateNewsScope(request.getScope(), request.getTargetRole(), request.getTargetBuildingId());

            if (request.getScope() == NotificationScope.INTERNAL) {
                news.setTargetRole(request.getTargetRole());
                news.setTargetBuildingId(null);
            } else if (request.getScope() == NotificationScope.EXTERNAL) {
                news.setTargetRole(null);
                news.setTargetBuildingId(request.getTargetBuildingId());
            }
        } else if (news.getScope() != null) {
            NotificationScope currentScope = news.getScope();
            validateNewsScope(currentScope, request.getTargetRole(), request.getTargetBuildingId());

            if (currentScope == NotificationScope.INTERNAL && request.getTargetRole() != null) {
                news.setTargetRole(request.getTargetRole());
            } else if (currentScope == NotificationScope.EXTERNAL && request.getTargetBuildingId() != null) {
                news.setTargetBuildingId(request.getTargetBuildingId());
            }
        }
        news.setUpdatedBy(userId);

        News updated = newsRepository.save(news);

        // Chỉ gửi realtime notification và FCM push khi:
        // 1. status = PUBLISHED
        // 2. scope = EXTERNAL (cho cư dân)
        // 3. publishAt <= now (không phải tương lai)
        if (shouldSendNotificationForNews(updated)) {
            WebSocketNewsMessage wsMessage = WebSocketNewsMessage.updated(
                    updated.getId(),
                    updated.getTitle(),
                    updated.getSummary(),
                    updated.getCoverImageUrl());
            notificationService.notifyNewsUpdated(wsMessage);
            notificationPushService.sendNewsUpdatedPush(updated);
            log.info("✅ [NewsService] Sent realtime and FCM push notification for updated news {} (PUBLISHED, EXTERNAL, publishAt <= now)", updated.getId());
        } else {
            log.info("⏭️ [NewsService] Skipped sending notification for updated news {} (status={}, scope={}, publishAt={})", 
                    updated.getId(), updated.getStatus(), updated.getScope(), updated.getPublishAt());
        }

        return toManagementResponse(updated);
    }

    public NewsManagementResponse deleteNews(UUID newsId, UUID userId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        news.setStatus(NewsStatus.ARCHIVED);
        news.setUpdatedBy(userId);

        News deleted = newsRepository.save(news);

        WebSocketNewsMessage wsMessage = WebSocketNewsMessage.deleted(deleted.getId());
        notificationService.notifyNewsDeleted(wsMessage);

        return toManagementResponse(deleted);
    }

    public List<NewsManagementResponse> getAllNews() {
        return newsRepository.findAll()
                .stream()
                .map(this::toManagementResponse)
                .collect(Collectors.toList());
    }

    public NewsManagementResponse getNewsDetail(UUID newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        return toManagementResponse(news);
    }


    public NewsPagedResponse getNewsForResidentPaged(UUID residentId, int page, int size) {
        // Lấy buildingId từ residentId
        UUID buildingId = getResidentBuildingId(residentId);
        
        log.info("🔍 [NewsService] getNewsForResidentPaged: residentId={}, buildingId={}, page={}, size={}", 
                residentId, buildingId, page, size);
        
        List<News> allNews = newsRepository.findAll();
        
        log.info("📰 [NewsService] Total news in DB: {}", allNews.size());
        
        // Log tất cả news trong DB để debug
        for (News news : allNews) {
            log.debug("📰 [NewsService] News in DB: id={}, title={}, status={}, scope={}, targetBuildingId={}, publishAt={}, expireAt={}", 
                    news.getId(), news.getTitle(), news.getStatus(), news.getScope(), 
                    news.getTargetBuildingId(), news.getPublishAt(), news.getExpireAt());
        }

        List<NewsDetailResponse> filteredAndSorted = allNews.stream()
                // Only show news with status = PUBLISHED (exclude DRAFT, SCHEDULED, HIDDEN, EXPIRED, ARCHIVED)
                .peek(news -> log.debug("📰 [NewsService] Checking news {}: status={}, scope={}, targetBuildingId={}", 
                        news.getId(), news.getStatus(), news.getScope(), news.getTargetBuildingId()))
                .filter(news -> {
                    boolean isPublished = news.getStatus() == NewsStatus.PUBLISHED;
                    if (!isPublished) {
                        log.debug("❌ [NewsService] News {} filtered out: status={} (not PUBLISHED)", 
                                news.getId(), news.getStatus());
                    }
                    return isPublished;
                })
                .filter(news -> {
                    // Filter theo buildingId: chỉ hiển thị news có targetBuildingId = null (tất cả tòa) 
                    // hoặc targetBuildingId = buildingId (tòa của resident)
                    boolean shouldShow = shouldShowNewsToBuilding(news, buildingId);
                    if (!shouldShow) {
                        log.info("❌ [NewsService] News {} filtered out: title={}, status={}, scope={}, targetBuildingId={}, publishAt={}, expireAt={}, buildingId={}", 
                                news.getId(), news.getTitle(), news.getStatus(), news.getScope(), 
                                news.getTargetBuildingId(), news.getPublishAt(), news.getExpireAt(), buildingId);
                    } else {
                        log.info("✅ [NewsService] News {} passed all filters: title={}, scope={}, targetBuildingId={}, publishAt={}, expireAt={}", 
                                news.getId(), news.getTitle(), news.getScope(), news.getTargetBuildingId(), 
                                news.getPublishAt(), news.getExpireAt());
                    }
                    return shouldShow;
                })
                .sorted((n1, n2) -> {
                    // Sort by publishAt DESC (newest first, from largest to smallest date)
                    // News with newest publishAt will be on page 1 (first page)
                    // If publishAt is null, fallback to createdAt
                    Instant publishAt1 = n1.getPublishAt() != null ? n1.getPublishAt() : n1.getCreatedAt();
                    Instant publishAt2 = n2.getPublishAt() != null ? n2.getPublishAt() : n2.getCreatedAt();
                    
                    if (publishAt1 != null && publishAt2 != null) {
                        // Sort DESC: publishAt2.compareTo(publishAt1) means newer date comes first
                        return publishAt2.compareTo(publishAt1);
                    }
                    // If one publishAt is null, prioritize the one with publishAt
                    if (publishAt1 != null) return -1;
                    if (publishAt2 != null) return 1;
                    return 0;
                })
                .map(this::toDetailResponse)
                .collect(Collectors.toList());

        log.info("✅ [NewsService] getNewsForResidentPaged: after filtering, found {} news for residentId={}, buildingId={}", 
                filteredAndSorted.size(), residentId, buildingId);

        // Calculate pagination
        long totalElements = filteredAndSorted.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Ensure page is within valid range
        if (page < 0) {
            page = 0;
        }
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }

        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filteredAndSorted.size());
        List<NewsDetailResponse> pagedContent = start < filteredAndSorted.size() 
                ? filteredAndSorted.subList(start, end)
                : new ArrayList<>();

        return NewsPagedResponse.builder()
                .content(pagedContent)
                .currentPage(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .isFirst(page == 0)
                .isLast(page >= totalPages - 1 || totalPages == 0)
                .build();
    }

    // Backward compatibility method - returns first page
    public List<NewsDetailResponse> getNewsForResident(UUID residentId) {
        NewsPagedResponse pagedResponse = getNewsForResidentPaged(residentId, 0, 7);
        return pagedResponse.getContent();
    }
    
    /**
     * Lấy buildingId từ residentId bằng cách:
     * 1. Lấy household members của resident
     * 2. Lấy household từ member
     * 3. Lấy unit từ household
     * 4. Lấy buildingId từ unit
     */
    private UUID getResidentBuildingId(UUID residentId) {
        if (residentId == null) {
            return null;
        }
        try {
            List<HouseholdMemberDto> members = baseServiceClient.getActiveHouseholdMembersByResident(residentId);
            if (members == null || members.isEmpty()) {
                log.warn("⚠️ [NewsService] No household members found for resident {}", residentId);
                return null;
            }

            // Ưu tiên primary member, nếu không có thì lấy member đầu tiên
            HouseholdMemberDto prioritizedMember = members.stream()
                    .filter(member -> Boolean.TRUE.equals(member.isPrimary()))
                    .findFirst()
                    .orElse(members.get(0));

            if (prioritizedMember.householdId() == null) {
                log.warn("⚠️ [NewsService] No householdId found for resident {}", residentId);
                return null;
            }

            HouseholdDto household = baseServiceClient.getHouseholdById(prioritizedMember.householdId());
            if (household == null || household.unitId() == null) {
                log.warn("⚠️ [NewsService] No unitId found for household {}", prioritizedMember.householdId());
                return null;
            }

            UnitDto unit = baseServiceClient.getUnitById(household.unitId());
            if (unit == null || unit.buildingId() == null) {
                log.warn("⚠️ [NewsService] No buildingId found for unit {}", household.unitId());
                return null;
            }

            log.info("✅ [NewsService] Resolved buildingId={} for residentId={}", unit.buildingId(), residentId);
            return unit.buildingId();
        } catch (Exception e) {
            log.warn("⚠️ [NewsService] Failed to resolve buildingId for resident {}: {}", residentId, e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra xem news có nên hiển thị cho building không.
     * 
     * Logic:
     * 1. Kiểm tra publishAt/expireAt dates
     * 2. Nếu scope == null → hiển thị cho tất cả
     * 3. Nếu scope == INTERNAL → không hiển thị cho resident (chỉ cho staff)
     * 4. Nếu scope == EXTERNAL:
     *    - Nếu targetBuildingId == null → hiển thị cho TẤT CẢ tòa
     *    - Nếu targetBuildingId != null → chỉ hiển thị cho tòa đó (so sánh với buildingId)
     */
    private boolean shouldShowNewsToBuilding(News news, UUID buildingId) {
        // Note: Status filter (PUBLISHED only) is already applied before calling this method
        // So we don't need to check isActive() here, but we still check publishAt/expireAt dates
        Instant now = Instant.now();
        
        // Check publishAt
        if (news.getPublishAt() != null && news.getPublishAt().isAfter(now)) {
            log.info("❌ [NewsService] News {} filtered by publishAt: publishAt={}, now={}", 
                    news.getId(), news.getPublishAt(), now);
            return false; // Not published yet
        }
        
        // Check expireAt
        if (news.getExpireAt() != null && news.getExpireAt().isBefore(now)) {
            log.info("❌ [NewsService] News {} filtered by expireAt: expireAt={}, now={}", 
                    news.getId(), news.getExpireAt(), now);
            return false; // Already expired
        }

        NotificationScope scope = news.getScope();
        
        // Nếu scope == null → hiển thị cho tất cả
        if (scope == null) {
            log.info("✅ [NewsService] News {} has no scope -> show to all", news.getId());
            return true;
        }

        // INTERNAL news chỉ dành cho staff, không hiển thị cho resident
        if (scope == NotificationScope.INTERNAL) {
            log.info("❌ [NewsService] News {} has scope=INTERNAL -> hide from residents", news.getId());
            return false;
        }

        // EXTERNAL news dành cho resident - filter theo buildingId
        if (scope == NotificationScope.EXTERNAL) {
            // Nếu targetBuildingId == null → hiển thị cho TẤT CẢ tòa
            if (news.getTargetBuildingId() == null) {
                log.info("✅ [NewsService] News {} has scope=EXTERNAL, targetBuildingId=null -> show to all buildings", news.getId());
                return true;
            }

            // Nếu targetBuildingId != null → chỉ hiển thị cho tòa đó
            if (buildingId == null) {
                // Nếu không có buildingId, chỉ hiển thị news có targetBuildingId = null
                // KHÔNG hiển thị news có targetBuildingId cụ thể
                log.warn("⚠️ [NewsService] No buildingId provided -> hiding news {} with targetBuildingId={}", 
                        news.getId(), news.getTargetBuildingId());
                return false;
            }
            
            boolean matches = buildingId.equals(news.getTargetBuildingId());
            if (matches) {
                log.info("✅ [NewsService] News {} has scope=EXTERNAL, targetBuildingId={} matches buildingId={}", 
                        news.getId(), news.getTargetBuildingId(), buildingId);
            } else {
                log.info("❌ [NewsService] News {} has scope=EXTERNAL, targetBuildingId={} doesn't match buildingId={}", 
                        news.getId(), news.getTargetBuildingId(), buildingId);
            }
            return matches;
        }

        // Default: allow access (backward compatibility)
        log.info("✅ [NewsService] News {} default case -> show to all", news.getId());
        return true;
    }

    public NewsDetailResponse getNewsForResident(UUID newsId, UUID residentId) {
        // Lấy buildingId từ residentId
        UUID buildingId = getResidentBuildingId(residentId);
        
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        if (!shouldShowNewsToBuilding(news, buildingId)) {
            throw new IllegalArgumentException("News not accessible for this resident");
        }

        return toDetailResponse(news);
    }


    private void validateNewsScope(NotificationScope scope, String targetRole, UUID targetBuildingId) {
        if (scope == null) {
            return;
        }

        if (scope == NotificationScope.INTERNAL) {
            if (targetRole == null || targetRole.isBlank()) {
                throw new IllegalArgumentException("INTERNAL news must have target_role (use 'ALL' for all roles)");
            }
            if (targetBuildingId != null) {
                throw new IllegalArgumentException("INTERNAL news cannot have target_building_id");
            }
        } else if (scope == NotificationScope.EXTERNAL) {
            if (targetRole != null && !targetRole.isBlank()) {
                throw new IllegalArgumentException("EXTERNAL news cannot have target_role");
            }
        }
    }

    /**
     * Kiểm tra xem có nên gửi notification (realtime + FCM push) cho news không.
     * Chỉ gửi khi:
     * 1. status = PUBLISHED
     * 2. scope = EXTERNAL (cho cư dân)
     * 3. publishAt <= now (không phải tương lai)
     */
    private boolean shouldSendNotificationForNews(News news) {
        // Chỉ gửi cho news có status PUBLISHED
        if (news.getStatus() != NewsStatus.PUBLISHED) {
            return false;
        }
        
        // Chỉ gửi cho news có scope EXTERNAL (cho cư dân)
        if (news.getScope() != NotificationScope.EXTERNAL) {
            return false;
        }
        
        // Chỉ gửi khi publishAt <= now (không phải tương lai)
        Instant now = Instant.now();
        if (news.getPublishAt() != null && news.getPublishAt().isAfter(now)) {
            return false; // publishAt là tương lai, không gửi notification
        }
        
        return true;
    }

    private NewsManagementResponse toManagementResponse(News news) {
        return NewsManagementResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .bodyHtml(news.getBodyHtml())
                .coverImageUrl(news.getCoverImageUrl())
                .status(news.getStatus())
                .publishAt(news.getPublishAt())
                .expireAt(news.getExpireAt())
                .displayOrder(news.getDisplayOrder())
                .scope(news.getScope())
                .targetRole(news.getTargetRole())
                .targetBuildingId(news.getTargetBuildingId())
                .viewCount(news.getViewCount())
                .images(toImageDtos(news.getImages()))
                .createdBy(news.getCreatedBy())
                .createdAt(news.getCreatedAt())
                .updatedBy(news.getUpdatedBy())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}