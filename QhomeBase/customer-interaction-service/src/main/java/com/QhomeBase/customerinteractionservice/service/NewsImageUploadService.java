package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.news.NewsImageDto;
import com.QhomeBase.customerinteractionservice.dto.news.UploadImageResponse;
import com.QhomeBase.customerinteractionservice.model.News;
import com.QhomeBase.customerinteractionservice.model.NewsImage;
import com.QhomeBase.customerinteractionservice.repository.NewsImageRepository;
import com.QhomeBase.customerinteractionservice.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NewsImageUploadService {
    private final NewsRepository newsRepository;
    private final NewsImageRepository newsImageRepository;

    public UploadImageResponse uploadImage(NewsImageDto newsImageDto) {
        log.info("Uploading image for news: {}", newsImageDto.getNewsId());

        validateImageDto(newsImageDto);

        UUID newsId = newsImageDto.getNewsId();
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        NewsImage newsImage = NewsImage.builder()
                .news(news)
                .url(newsImageDto.getUrl())
                .caption(newsImageDto.getCaption())
                .sortOrder(newsImageDto.getSortOrder() != null ? newsImageDto.getSortOrder() : 0)
                .fileSize(newsImageDto.getFileSize())
                .contentType(newsImageDto.getContentType())
                .createdAt(Instant.now())
                .build();

        NewsImage saved = newsImageRepository.save(newsImage);

        return toUploadImageResponse(saved);
    }

    public List<UploadImageResponse> uploadMultipleImages(List<NewsImageDto> imageDtos) {
        if (imageDtos == null || imageDtos.isEmpty()) {
            throw new IllegalArgumentException("Image list cannot be empty");
        }

        UUID newsId = imageDtos.get(0).getNewsId();
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        List<NewsImage> newsImages = new ArrayList<>();
        for (NewsImageDto dto : imageDtos) {
            validateImageDto(dto);

            if (!dto.getNewsId().equals(newsId)) {
                throw new IllegalArgumentException("All images must belong to the same news");
            }

            NewsImage newsImage = NewsImage.builder()
                    .news(news)
                    .url(dto.getUrl())
                    .caption(dto.getCaption())
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                    .fileSize(dto.getFileSize())
                    .contentType(dto.getContentType())
                    .createdAt(Instant.now())
                    .build();

            newsImages.add(newsImage);
        }

        List<NewsImage> savedImages = newsImageRepository.saveAll(newsImages);

        return savedImages.stream()
                .map(this::toUploadImageResponse)
                .collect(Collectors.toList());
    }

    public UploadImageResponse updateImageCaption(UUID imageId, String caption) {
        NewsImage newsImage = newsImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));

        newsImage.setCaption(caption);
        NewsImage updated = newsImageRepository.save(newsImage);
        return toUploadImageResponse(updated);
    }

    public void deleteImage(UUID imageId) {
        log.info("Deleting image: {}", imageId);

        NewsImage newsImage = newsImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));

        newsImageRepository.delete(newsImage);
        log.info("Image deleted successfully: {}", imageId);
    }

    public List<UploadImageResponse> getImagesByNewsId(UUID newsId) {
        log.info("Getting images for news: {}", newsId);

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with ID: " + newsId));

        List<NewsImage> images = news.getImages();

        return images.stream()
                .map(this::toUploadImageResponse)
                .collect(Collectors.toList());
    }

    public UploadImageResponse updateSortOrder(UUID imageId, Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("Sort order must be non-negative");
        }

        NewsImage newsImage = newsImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with ID: " + imageId));

        UUID newsId = newsImage.getNews().getId();
        List<NewsImage> allImages = newsImageRepository.findByNewsId(newsId);

        allImages.sort(Comparator.comparing(NewsImage::getSortOrder));

        allImages.removeIf(img -> img.getId().equals(imageId));

        if (sortOrder > allImages.size()) {
            sortOrder = allImages.size();
        }

        allImages.add(sortOrder, newsImage);

        for (int i = 0; i < allImages.size(); i++) {
            allImages.get(i).setSortOrder(i);
        }

        newsImageRepository.saveAll(allImages);

        return toUploadImageResponse(newsImage);
    }

    private void validateImageDto(NewsImageDto dto) {
        if (dto.getNewsId() == null) {
            throw new IllegalArgumentException("News ID is required");
        }
        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL is required");
        }
    }

    private UploadImageResponse toUploadImageResponse(NewsImage newsImage) {
        return new UploadImageResponse(
                newsImage.getUrl(),
                getFileName(newsImage.getUrl()),
                newsImage.getFileSize(),
                newsImage.getContentType()
        );
    }

    private String getFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }
}