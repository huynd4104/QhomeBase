package com.QhomeBase.marketplaceservice.controller;

import com.QhomeBase.marketplaceservice.service.ImageKitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Proxy controller to stream videos from ImageKit
 * This solves the 403 error issue with ExoPlayer by proxying video requests through backend
 * The backend can add proper headers when fetching from ImageKit, and client only needs to request from our backend
 */
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
public class MarketplaceMediaProxyController {

    private final ImageKitService imageKitService;

    /**
     * Proxy endpoint to stream video from ImageKit
     * Usage: GET /api/marketplace/media/video?url={encodedImageKitUrl}
     * (API Gateway strips /api/marketplace prefix, so service receives /media/video)
     * 
     * IMPORTANT: This endpoint requires Hotlink Protection to be DISABLED in ImageKit Dashboard
     * OR the backend server domain/IP must be added to ImageKit's whitelist.
     * 
     * Steps to fix 403 errors:
     * 1. Go to ImageKit Dashboard → Settings → Security
     * 2. Disable "Hotlink Protection" OR add your backend server domain/IP to whitelist
     * 3. Restart marketplace-service
     * 
     * This endpoint:
     * 1. Receives ImageKit video URL from client
     * 2. Generates signed URL (though it may not work for server-to-server requests)
     * 3. Fetches video from ImageKit and streams back to client
     * 
     * Supports Range requests for video seeking
     */
    @GetMapping("/video")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            @RequestParam("url") String imageKitUrl,
            @RequestHeader(value = "Range", required = false) String rangeHeader) {
        
        try {
            log.info("📹 [MediaProxy] Streaming video from ImageKit: {}, Range: {}", imageKitUrl, rangeHeader);
            
            // Validate that URL is from ImageKit
            if (!imageKitService.isImageKitUrl(imageKitUrl)) {
                log.warn("⚠️ [MediaProxy] URL is not from ImageKit: {}", imageKitUrl);
                return ResponseEntity.badRequest().build();
            }
            
            // Try to get signed URL (may not work for server-to-server, but worth trying)
            String finalImageKitUrl = imageKitService.getSignedUrl(imageKitUrl, 3600L);
            log.info("📹 [MediaProxy] Using ImageKit URL (signed): {}", finalImageKitUrl);
            
            // Create HTTP connection to ImageKit
            URI imageKitUri = new URI(finalImageKitUrl);
            URL url = imageKitUri.toURL();
            HttpURLConnection initialConnection = (HttpURLConnection) url.openConnection();
            
            // Set request headers - try to mimic a browser request
            initialConnection.setRequestMethod("GET");
            initialConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            initialConnection.setRequestProperty("Accept", "*/*");
            initialConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            initialConnection.setRequestProperty("Connection", "keep-alive");
            initialConnection.setRequestProperty("Referer", "https://ik.imagekit.io/");
            initialConnection.setRequestProperty("Origin", "https://ik.imagekit.io");
            
            // Forward Range header if present (for video seeking)
            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                initialConnection.setRequestProperty("Range", rangeHeader);
                log.debug("📹 [MediaProxy] Forwarding Range header: {}", rangeHeader);
            }
            
            // Don't follow redirects automatically
            initialConnection.setInstanceFollowRedirects(false);
            
            // Connect to ImageKit
            initialConnection.connect();
            
            int responseCode = initialConnection.getResponseCode();
            
            // Handle redirects (3xx) - create final connection variable
            final HttpURLConnection finalConnection;
            if (responseCode >= 300 && responseCode < 400) {
                String location = initialConnection.getHeaderField("Location");
                if (location != null) {
                    log.info("📹 [MediaProxy] Following redirect to: {}", location);
                    initialConnection.disconnect();
                    // Retry with redirect URL
                    URI redirectUri = new URI(location);
                    URL redirectUrl = redirectUri.toURL();
                    HttpURLConnection redirectConnection = (HttpURLConnection) redirectUrl.openConnection();
                    redirectConnection.setRequestMethod("GET");
                    redirectConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                    redirectConnection.setRequestProperty("Accept", "*/*");
                    if (rangeHeader != null && !rangeHeader.isEmpty()) {
                        redirectConnection.setRequestProperty("Range", rangeHeader);
                    }
                    redirectConnection.connect();
                    responseCode = redirectConnection.getResponseCode();
                    finalConnection = redirectConnection;
                } else {
                    finalConnection = initialConnection;
                }
            } else {
                finalConnection = initialConnection;
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                // Get content type from ImageKit response
                String contentType = finalConnection.getContentType();
                if (contentType == null || contentType.isEmpty()) {
                    contentType = "video/mp4"; // Default to video/mp4
                }
                
                // Get content length if available
                long contentLength = finalConnection.getContentLengthLong();
                
                log.info("✅ [MediaProxy] Successfully connected to ImageKit, content-type: {}, size: {}", 
                        contentType, contentLength > 0 ? contentLength : "unknown");
                
                // Build response headers
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.parseMediaType(contentType));
                if (contentLength > 0) {
                    responseHeaders.setContentLength(contentLength);
                }
                // Enable range requests for video seeking
                responseHeaders.set("Accept-Ranges", "bytes");
                // Cache control - cache for 1 hour
                responseHeaders.setCacheControl("public, max-age=3600");
                
                // Handle partial content (206) if Range header was present
                HttpStatus status = (rangeHeader != null && responseCode == HttpStatus.PARTIAL_CONTENT.value())
                        ? HttpStatus.PARTIAL_CONTENT
                        : HttpStatus.OK;
                
                // Copy range response headers if present
                String contentRange = finalConnection.getHeaderField("Content-Range");
                if (contentRange != null) {
                    responseHeaders.set("Content-Range", contentRange);
                }
                
                // Get input stream from ImageKit
                InputStream inputStream = finalConnection.getInputStream();
                
                // Create StreamingResponseBody to stream video to client
                StreamingResponseBody responseBody = outputStream -> {
                    try {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.flush();
                    } finally {
                        inputStream.close();
                        finalConnection.disconnect();
                    }
                };
                
                return ResponseEntity.status(status)
                        .headers(responseHeaders)
                        .body(responseBody);
                        
            } else {
                log.error("❌ [MediaProxy] Failed to fetch video from ImageKit: status={}", responseCode);
                log.error("❌ [MediaProxy] Response message: {}", finalConnection.getResponseMessage());
                log.error("❌ [MediaProxy] ⚠️ SOLUTION: Disable Hotlink Protection in ImageKit Dashboard OR add backend domain to whitelist");
                finalConnection.disconnect();
                return ResponseEntity.status(responseCode).build();
            }
            
        } catch (Exception e) {
            log.error("❌ [MediaProxy] Error streaming video from ImageKit: {}", e.getMessage(), e);
            log.error("❌ [MediaProxy] ⚠️ SOLUTION: Disable Hotlink Protection in ImageKit Dashboard OR add backend domain to whitelist");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

