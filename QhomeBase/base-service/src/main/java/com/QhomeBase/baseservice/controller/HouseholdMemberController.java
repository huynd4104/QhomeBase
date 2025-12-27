package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.HouseholdMemberDto;
import com.QhomeBase.baseservice.service.HouseHoldMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/household-members")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class HouseholdMemberController {

    private final HouseHoldMemberService householdMemberService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<HouseholdMemberDto> getHouseholdMemberById(@PathVariable UUID id) {
        try {
            HouseholdMemberDto result = householdMemberService.getHouseholdMemberById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get household member {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/households/{householdId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<?> getActiveMembersByHouseholdId(
            @PathVariable UUID householdId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        try {
            // If pagination parameters are provided, return paginated response
            if (limit != null || offset != null) {
                java.util.Map<String, Object> pagedResponse = householdMemberService
                        .getActiveMembersByHouseholdIdPaged(householdId, limit, offset);
                return ResponseEntity.ok(pagedResponse);
            }
            // Otherwise, return all members (backward compatibility)
            List<HouseholdMemberDto> result = householdMemberService.getActiveMembersByHouseholdId(householdId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid household ID {}: {}", householdId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to get members for household {}: {}", householdId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/residents/{residentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<HouseholdMemberDto>> getActiveMembersByResidentId(@PathVariable UUID residentId) {
        try {
            List<HouseholdMemberDto> result = householdMemberService.getActiveMembersByResidentId(residentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Failed to get households for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/residents/{residentId}/household-info")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<?> getHouseholdInfoWithMembersByResidentId(@PathVariable UUID residentId) {
        try {
            java.util.Map<String, Object> result = householdMemberService.getHouseholdInfoWithMembersByResidentId(residentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Failed to get household info for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/households/{householdId}/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<HouseholdMemberDto>> getAllMembersByHouseholdId(@PathVariable UUID householdId) {
        try {
            List<HouseholdMemberDto> result = householdMemberService.getAllMembersByHouseholdId(householdId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid household ID {}: {}", householdId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to get all members for household {}: {}", householdId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

