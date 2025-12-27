package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.ResidentAccountDto;
import com.QhomeBase.baseservice.repository.ResidentRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.IamClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final IamClientService iamClientService;
    private final ResidentRepository residentRepository;
    
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        try {
            if (authentication == null) {
                log.error("Authentication is null!");
                return ResponseEntity.status(500).body(Map.of("error", "Authentication is null"));
            }
            
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();
            
            // Get user account info from IAM service with timeout handling
            ResidentAccountDto accountDto;
            try {
                accountDto = iamClientService.getUserAccountInfo(userId);
            } catch (Exception e) {
                // IAM service timeout or error - return fallback response
                log.warn("IAM service call failed for userId {}: {}", userId, e.getMessage());
                Map<String, Object> fallbackResponse = new HashMap<>();
                fallbackResponse.put("id", userId.toString());
                fallbackResponse.put("username", "User");
                fallbackResponse.put("email", "");
                fallbackResponse.put("roles", List.of());
                fallbackResponse.put("active", true);
                fallbackResponse.put("fullName", "User");
                return ResponseEntity.ok(fallbackResponse);
            }
            
            if (accountDto == null) {
                log.warn("User account not found in IAM service for userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
            // Get resident info if exists
            UUID residentId = null;
            String fullName = null;
            String phone = null;
            String nationalId = null;
            java.time.LocalDate dob = null;
            try {
                var residentOpt = residentRepository.findByUserId(userId);
                if (residentOpt.isPresent()) {
                    var resident = residentOpt.get();
                    residentId = resident.getId();
                    fullName = resident.getFullName();
                    phone = resident.getPhone();
                    nationalId = resident.getNationalId();
                    dob = resident.getDob();
                }
                log.debug("Resident info for userId {}: residentId={}, fullName={}", userId, residentId, fullName);
            } catch (Exception e) {
                log.warn("Error finding resident for userId {}: {}", userId, e.getMessage());
                // Continue without resident info
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", userId.toString());
            response.put("username", accountDto.username());
            response.put("email", accountDto.email());
            response.put("roles", accountDto.roles());
            response.put("active", accountDto.active());
            if (residentId != null) {
                response.put("residentId", residentId.toString());
            }
            // Always include fullName, use username as fallback if fullName is null
            if (fullName != null && !fullName.trim().isEmpty()) {
                response.put("fullName", fullName);
            } else {
                // Fallback to username if fullName is not available
                response.put("fullName", accountDto.username() != null ? accountDto.username() : "User");
            }
            if (phone != null) {
                response.put("phoneNumber", phone);
            }
            if (nationalId != null) {
                response.put("citizenId", nationalId);
                response.put("identityNumber", nationalId);
            }
            if (dob != null) {
                response.put("dateOfBirth", dob.toString());
            }
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Only log stacktrace for >=500 errors (production-ready)
            log.error("Runtime error getting current user: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get user info: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        } catch (Exception e) {
            // Only log stacktrace for unexpected errors (production-ready)
            log.error("Unexpected error getting current user: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{userId}/email")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public ResponseEntity<String> getUserEmail(@PathVariable UUID userId) {
        try {
            ResidentAccountDto accountDto = iamClientService.getUserAccountInfo(userId);
            if (accountDto == null) {
                log.warn("User account not found in IAM service for userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(accountDto.email() != null ? accountDto.email() : "");
        } catch (Exception e) {
            log.warn("Error getting user email for userId {}: {}", userId, e.getMessage());
            return ResponseEntity.status(500).body("");
        }
    }

    @GetMapping("/check-username/{username}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> checkUsernameAvailability(@PathVariable String username) {
        try {
            boolean exists = iamClientService.usernameExists(username);
            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "available", !exists,
                    "message", exists 
                            ? "Username '" + username + "' đã được sử dụng trong hệ thống. Vui lòng chọn username khác."
                            : "Username có thể sử dụng."
            ));
        } catch (Exception e) {
            log.warn("Error checking username availability for {}: {}", username, e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "username", username,
                    "available", false,
                    "message", "Không thể kiểm tra username. Vui lòng thử lại."
            ));
        }
    }
}

