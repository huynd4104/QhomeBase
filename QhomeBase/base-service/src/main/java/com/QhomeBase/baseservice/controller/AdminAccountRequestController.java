package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.AccountCreationRequestDto;
import com.QhomeBase.baseservice.dto.ApproveAccountRequestDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.ResidentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/account-requests")
@RequiredArgsConstructor
@Slf4j
public class AdminAccountRequestController {
    
    private final ResidentAccountService residentAccountService;
    
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AccountCreationRequestDto>> getPendingRequests() {
        try {
            List<AccountCreationRequestDto> requests = residentAccountService.getPendingRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.warn("Failed to get pending requests: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountCreationRequestDto> approveAccountRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody ApproveAccountRequestDto request,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID adminUserId = principal.uid();
            String token = principal.token();
            
            AccountCreationRequestDto result = residentAccountService.approveAccountRequest(
                    requestId,
                    adminUserId,
                    request.approve(),
                    request.rejectionReason(),
                    token
            );
            
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to approve/reject request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error approving/rejecting request {}: {}", requestId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}

