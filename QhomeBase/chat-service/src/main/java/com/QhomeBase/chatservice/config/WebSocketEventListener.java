package com.QhomeBase.chatservice.config;

import com.QhomeBase.chatservice.security.UserPrincipal;
import com.QhomeBase.chatservice.service.ResidentInfoService;
import com.QhomeBase.chatservice.service.WebSocketPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.UUID;

/**
 * Event listener for WebSocket connection events
 * Tracks user presence when they connect/disconnect
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketPresenceService presenceService;
    private final ResidentInfoService residentInfoService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // Try to get user from authentication
        Authentication auth = (Authentication) headerAccessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            UUID userId = principal.uid();
            String accessToken = principal.token();
            
            // Convert userId to residentId
            UUID residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId != null) {
                presenceService.registerSession(residentId, sessionId);
                log.info("✅ [WebSocketEventListener] User connected - userId: {}, residentId: {}, sessionId: {}", 
                        userId, residentId, sessionId);
            } else {
                log.warn("⚠️ [WebSocketEventListener] Could not find residentId for userId: {}", userId);
            }
        } else {
            log.debug("🔍 [WebSocketEventListener] WebSocket connected without authentication - sessionId: {}", sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // Try to get user from authentication first
        Authentication auth = (Authentication) headerAccessor.getUser();
        UUID residentId = null;
        
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            UUID userId = principal.uid();
            String accessToken = principal.token();
            
            // Convert userId to residentId
            residentId = residentInfoService.getResidentIdFromUserId(userId, accessToken);
            if (residentId != null) {
                presenceService.unregisterSession(residentId, sessionId);
                log.info("👋 [WebSocketEventListener] User disconnected - userId: {}, residentId: {}, sessionId: {}", 
                        userId, residentId, sessionId);
            } else {
                log.warn("⚠️ [WebSocketEventListener] Could not find residentId for userId: {} on disconnect", userId);
            }
        }
        
        // If we couldn't get residentId from auth, try to look it up from sessionId
        // This handles cases where authentication is lost on disconnect
        if (residentId == null) {
            presenceService.unregisterSessionBySessionId(sessionId);
            log.debug("🔍 [WebSocketEventListener] WebSocket disconnected - looked up residentId from sessionId: {}", sessionId);
        }
    }

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        log.debug("📡 [WebSocketEventListener] User subscribed to: {} (sessionId: {})", destination, sessionId);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.debug("📡 [WebSocketEventListener] User unsubscribed (sessionId: {})", sessionId);
    }
}
