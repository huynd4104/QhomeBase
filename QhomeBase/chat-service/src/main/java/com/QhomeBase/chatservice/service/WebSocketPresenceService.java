package com.QhomeBase.chatservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track WebSocket presence for users
 * Tracks which users are currently connected via WebSocket
 */
@Service
@Slf4j
public class WebSocketPresenceService {

    // Map of residentId -> Set of sessionIds
    // A user can have multiple sessions (multiple devices)
    private final ConcurrentHashMap<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();
    
    // Map of sessionId -> residentId (for quick lookup on disconnect)
    private final ConcurrentHashMap<String, UUID> sessionToResident = new ConcurrentHashMap<>();

    /**
     * Register a WebSocket session for a user
     * @param residentId The resident ID
     * @param sessionId The WebSocket session ID
     */
    public void registerSession(UUID residentId, String sessionId) {
        userSessions.computeIfAbsent(residentId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionToResident.put(sessionId, residentId);
        log.debug("📱 [WebSocketPresenceService] Registered session {} for residentId: {} (total sessions: {})", 
                sessionId, residentId, userSessions.get(residentId).size());
    }

    /**
     * Unregister a WebSocket session for a user
     * @param residentId The resident ID (can be null, will be looked up from sessionId)
     * @param sessionId The WebSocket session ID
     */
    public void unregisterSession(UUID residentId, String sessionId) {
        // If residentId is not provided, try to look it up from sessionId
        if (residentId == null) {
            residentId = sessionToResident.get(sessionId);
            if (residentId == null) {
                log.warn("⚠️ [WebSocketPresenceService] Could not find residentId for sessionId: {}", sessionId);
                return;
            }
        }
        
        Set<String> sessions = userSessions.get(residentId);
        if (sessions != null) {
            sessions.remove(sessionId);
            sessionToResident.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(residentId);
                log.debug("📱 [WebSocketPresenceService] Removed all sessions for residentId: {} (user is now offline)", residentId);
            } else {
                log.debug("📱 [WebSocketPresenceService] Removed session {} for residentId: {} (remaining sessions: {})", 
                        sessionId, residentId, sessions.size());
            }
        } else {
            // Clean up sessionToResident even if sessions not found
            sessionToResident.remove(sessionId);
        }
    }
    
    /**
     * Unregister a WebSocket session by sessionId only
     * @param sessionId The WebSocket session ID
     */
    public void unregisterSessionBySessionId(String sessionId) {
        UUID residentId = sessionToResident.get(sessionId);
        if (residentId != null) {
            unregisterSession(residentId, sessionId);
        } else {
            log.warn("⚠️ [WebSocketPresenceService] Could not find residentId for sessionId: {} on disconnect", sessionId);
        }
    }

    /**
     * Check if a user is currently connected via WebSocket
     * @param residentId The resident ID
     * @return true if user has at least one active WebSocket session
     */
    public boolean isUserOnline(UUID residentId) {
        Set<String> sessions = userSessions.get(residentId);
        boolean isOnline = sessions != null && !sessions.isEmpty();
        log.debug("📱 [WebSocketPresenceService] User {} is {} (sessions: {})", 
                residentId, isOnline ? "ONLINE" : "OFFLINE", sessions != null ? sessions.size() : 0);
        return isOnline;
    }

    /**
     * Get the number of active sessions for a user
     * @param residentId The resident ID
     * @return Number of active sessions
     */
    public int getActiveSessionCount(UUID residentId) {
        Set<String> sessions = userSessions.get(residentId);
        return sessions != null ? sessions.size() : 0;
    }

    /**
     * Remove all sessions for a user (e.g., on logout)
     * @param residentId The resident ID
     */
    public void removeAllSessions(UUID residentId) {
        Set<String> sessions = userSessions.remove(residentId);
        if (sessions != null) {
            // Clean up sessionToResident mapping
            sessions.forEach(sessionToResident::remove);
        }
        log.debug("📱 [WebSocketPresenceService] Removed all sessions for residentId: {}", residentId);
    }
}
