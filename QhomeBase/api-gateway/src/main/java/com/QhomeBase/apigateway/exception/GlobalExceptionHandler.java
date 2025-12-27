package com.QhomeBase.apigateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Global exception handler for API Gateway
 * Handles common exceptions including WebSocket connection errors
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle WebSocket connection reset errors gracefully
     * These are common when clients close connections abruptly (refresh, close tab, etc.)
     */
    public static Mono<Void> handleWebSocketError(Throwable throwable) {
        if (throwable instanceof IOException) {
            IOException ioException = (IOException) throwable;
            String message = ioException.getMessage();
            
            // Connection reset by peer is a normal scenario when client closes connection
            if (message != null && (
                message.contains("Connection reset by peer") ||
                message.contains("Broken pipe") ||
                message.contains("Connection closed") ||
                message.contains("aborted")
            )) {
                // Log at DEBUG level instead of ERROR to reduce noise
                log.debug("WebSocket connection closed by client: {}", message);
                return Mono.empty();
            }
        }
        
        // For other errors, log at WARN level
        log.warn("WebSocket error: {}", throwable.getMessage());
        return Mono.empty();
    }
}

