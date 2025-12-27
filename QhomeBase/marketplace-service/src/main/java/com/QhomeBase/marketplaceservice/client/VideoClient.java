package com.QhomeBase.marketplaceservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Client to call Video Storage Service (data-docs-service)
 * Handles video upload and retrieval
 */
@Component
@Slf4j
public class VideoClient {

    private final RestTemplate restTemplate;
    
    @Value("${services.data-docs.base-url:http://localhost:8082}")
    private String dataDocsServiceUrl;

    public VideoClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Upload video to data-docs-service
     * @return VideoUploadResponse containing videoId and streaming URL
     */
    public Map<String, Object> uploadVideo(
            MultipartFile file,
            String category,
            UUID ownerId,
            UUID uploadedBy) throws IOException {
        
        String uploadUrl = dataDocsServiceUrl + "/api/videos/upload";
        
        log.info("📤 [VideoClient] Uploading video to data-docs-service: url={}, category={}, size={} MB",
                uploadUrl, category, file.getSize() / (1024.0 * 1024.0));
        
        try {
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("category", category);
            if (ownerId != null) {
                body.add("ownerId", ownerId.toString());
            }
            body.add("uploadedBy", uploadedBy.toString());

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                log.info("✅ [VideoClient] Video uploaded successfully: videoId={}", responseBody.get("videoId"));
                return responseBody;
            } else {
                throw new RestClientException("Failed to upload video: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("❌ [VideoClient] Failed to upload video to data-docs-service", e);
            throw new IOException("Failed to upload video: " + e.getMessage(), e);
        }
    }

    /**
     * Get video streaming URL (relative path)
     * Returns relative path - client should prepend base URL from app_config.dart
     * @param videoId UUID of the video
     * @return Relative path for the video stream (e.g., "/api/videos/stream/{videoId}")
     */
    public String getVideoStreamingUrl(UUID videoId) {
        // Return relative path - Flutter app will prepend base URL from app_config.dart
        // This allows easy URL updates without database changes
        return "/api/videos/stream/" + videoId;
    }
}
