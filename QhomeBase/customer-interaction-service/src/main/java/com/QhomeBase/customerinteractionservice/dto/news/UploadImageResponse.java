package com.QhomeBase.customerinteractionservice.dto.news;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageResponse {

    private String url;
    private String filename;
    private Long size;
    private String contentType;

    public static UploadImageResponse of(String url, String filename, Long size, String contentType) {
        return UploadImageResponse.builder()
                .url(url)
                .filename(filename)
                .size(size)
                .contentType(contentType)
                .build();
    }
}

