package com.QhomeBase.customerinteractionservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadClient {
    
    @Value("${data-docs-service.url:http://localhost:8082}")
    private String dataDocsServiceUrl;
    
    public String getUploadEndpoint() {
        return dataDocsServiceUrl + "/api/files/upload/news-image";
    }
    
    public String getMultipleUploadEndpoint() {
        return dataDocsServiceUrl + "/api/files/upload/news-images";
    }
}

