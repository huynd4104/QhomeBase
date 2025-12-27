package com.QhomeBase.customerinteractionservice.service;

import com.QhomeBase.customerinteractionservice.dto.news.WebSocketNewsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    public void notifyNewsCreated(WebSocketNewsMessage message) {
        sendMessage(message, "NEWS_CREATED");
    }
    
    public void notifyNewsUpdated(WebSocketNewsMessage message) {
        sendMessage(message, "NEWS_UPDATED");
    }
    
    public void notifyNewsDeleted(WebSocketNewsMessage message) {
        sendMessage(message, "NEWS_DELETED");
    }
    
    private void sendMessage(WebSocketNewsMessage message, String action) {
        String destination = "/topic/news";
        
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("❌ [NewsNotificationService] Error sending WebSocket news notification | Action: {} | Destination: {} | NewsId: {} | Error: {}", 
                    action, destination, message.getNewsId(), e.getMessage(), e);
        }
    }
}

