package com.QhomeBase.financebillingservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage(), ex);
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("error", "IllegalArgumentException");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .headers(headers)
                .body(errorResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException: {}", ex.getMessage(), ex);
        
        String message = ex.getMessage();
        String userMessage = "Dữ liệu đã tồn tại trong hệ thống";
        
        // Check if it's a duplicate key violation for billing cycles
        if (message != null && message.contains("uq_billing_cycles")) {
            userMessage = "Chu kỳ thanh toán với cùng tên và khoảng thời gian đã tồn tại. Vui lòng kiểm tra lại.";
        } else if (message != null && message.contains("duplicate key")) {
            userMessage = "Dữ liệu đã tồn tại trong hệ thống. Vui lòng kiểm tra lại.";
        }
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", userMessage);
        errorResponse.put("error", "DataIntegrityViolationException");
        errorResponse.put("details", message != null && message.length() > 200 
                ? message.substring(0, 200) + "..." 
                : message);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8");
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .headers(headers)
                .body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Đã xảy ra lỗi không mong muốn: " + ex.getMessage());
        errorResponse.put("error", "InternalServerError");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

