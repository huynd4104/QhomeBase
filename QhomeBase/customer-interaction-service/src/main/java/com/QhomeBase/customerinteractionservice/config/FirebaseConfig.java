package com.QhomeBase.customerinteractionservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account}")
    private Resource serviceAccountResource;

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                try (InputStream stream = serviceAccountResource.getInputStream()) {
                    log.info("📄 Loading Firebase service account from: {}", serviceAccountResource.getURI());
                    
                    GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
                    
                    try {
                        if (credentials.createScopedRequired()) {
                            credentials = credentials.createScoped(
                                "https://www.googleapis.com/auth/firebase.messaging",
                                "https://www.googleapis.com/auth/cloud-platform"
                            );
                            log.info("✅ Credentials scoped for Firebase Cloud Messaging");
                        }
                        
                        credentials.refresh();
                        log.info("✅ Firebase credentials refreshed successfully");
                    } catch (Exception refreshEx) {
                        log.error("⚠️ Failed to refresh credentials: {}", refreshEx.getMessage());
                        log.error("   Error details: {}", refreshEx.getClass().getName());
                        if (refreshEx.getCause() != null) {
                            log.error("   Cause: {}", refreshEx.getCause().getMessage());
                        }
                        log.error("   Service account key may be invalid or revoked.");
                        log.error("   Push notifications will be disabled. Please generate a new service account key.");
                        return null;
                    }
                    
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("✅ FirebaseApp initialized successfully with project: {}", options.getProjectId());
                    
                    try {
                        FirebaseMessaging.getInstance();
                        log.info("✅ FirebaseMessaging instance created successfully");
                    } catch (Exception testEx) {
                        log.error("⚠️ Failed to create FirebaseMessaging instance: {}", testEx.getMessage());
                        return null;
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to initialize FirebaseApp: {}", e.getMessage());
                    log.error("   Error type: {}", e.getClass().getName());
                    if (e.getCause() != null) {
                        log.error("   Cause: {}", e.getCause().getMessage());
                    }
                    log.error("   ⚠️ Push notifications will be disabled. Backend will continue to run.");
                    log.error("   💡 To fix: Generate a new service account key from Firebase Console and replace the file.");
                    return null; 
                }
            } else {
                log.info("✅ FirebaseApp already initialized");
            }
            return FirebaseMessaging.getInstance();
        } catch (Exception e) {
            log.error("❌ Unexpected error initializing Firebase: {}", e.getMessage(), e);
            log.error("   ⚠️ Push notifications will be disabled. Backend will continue to run.");
            return null;
        }
    }
}

