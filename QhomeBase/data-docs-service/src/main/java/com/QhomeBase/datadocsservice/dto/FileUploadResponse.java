package com.QhomeBase.datadocsservice.dto;

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
public class FileUploadResponse {
    
    private UUID fileId;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    private UUID uploadedBy;
    private Instant uploadedAt;
    
    public static FileUploadResponse success(
            UUID fileId,
            String fileName,
            String originalFileName,
            String fileUrl,
            String contentType,
            Long fileSize,
            UUID uploadedBy) {
        return FileUploadResponse.builder()
                .fileId(fileId)
                .fileName(fileName)
                .originalFileName(originalFileName)
                .fileUrl(fileUrl)
                .contentType(contentType)
                .fileSize(fileSize)
                .uploadedBy(uploadedBy)
                .uploadedAt(Instant.now())
                .build();
    }
}






