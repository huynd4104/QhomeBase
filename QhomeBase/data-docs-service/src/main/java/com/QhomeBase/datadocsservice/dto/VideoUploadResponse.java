package com.QhomeBase.datadocsservice.dto;

import com.QhomeBase.datadocsservice.model.VideoStorage;
import java.util.UUID;

public record VideoUploadResponse(
        UUID id,
        String fileName,
        String originalFileName,
        String fileUrl,
        String contentType,
        Long fileSize,
        String category,
        UUID ownerId,
        String resolution,
        Integer durationSeconds,
        Integer width,
        Integer height,
        UUID uploadedBy
) {
    public static VideoUploadResponse from(VideoStorage videoStorage) {
        return new VideoUploadResponse(
                videoStorage.getId(),
                videoStorage.getFileName(),
                videoStorage.getOriginalFileName(),
                videoStorage.getFileUrl(),
                videoStorage.getContentType(),
                videoStorage.getFileSize(),
                videoStorage.getCategory(),
                videoStorage.getOwnerId(),
                videoStorage.getResolution(),
                videoStorage.getDurationSeconds(),
                videoStorage.getWidth(),
                videoStorage.getHeight(),
                videoStorage.getUploadedBy()
        );
    }
}
