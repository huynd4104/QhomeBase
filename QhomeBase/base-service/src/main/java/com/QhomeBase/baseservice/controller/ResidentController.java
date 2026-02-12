package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.ResidentAccountService;
import com.QhomeBase.baseservice.service.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
@Slf4j
public class ResidentController {

    private final ResidentAccountService residentAccountService;
    private final ResidentService residentService;

    @GetMapping("/units/{unitId}/household/members/without-account")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> getResidentsWithoutAccount(
            @PathVariable UUID unitId,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();

            List<ResidentWithoutAccountDto> residents = residentAccountService
                    .getResidentsWithoutAccount(unitId, requesterUserId);

            return ResponseEntity.ok(residents);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get residents without account: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/create-account-request")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> createAccountRequest(
            @Valid @RequestBody CreateAccountRequestDto request,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();

            AccountCreationRequestDto accountRequest = residentAccountService
                    .createAccountRequest(request, requesterUserId);

            return ResponseEntity.status(HttpStatus.CREATED).body(accountRequest);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create account request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-account-requests")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> getMyAccountRequests(
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();

            List<AccountCreationRequestDto> requests = residentAccountService
                    .getMyRequests(requesterUserId);

            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.warn("Failed to get account requests: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/account-requests/{requestId}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> cancelMyAccountRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();

            AccountCreationRequestDto dto = residentAccountService
                    .cancelAccountRequest(requestId, requesterUserId);

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to cancel account request {}: {}", requestId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{residentId}/account")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<?> getResidentAccount(
            @PathVariable UUID residentId,
            Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID requesterUserId = principal.uid();

            ResidentAccountDto account = residentAccountService
                    .getResidentAccount(residentId, requesterUserId);

            if (account == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(account);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get account for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-units")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<UnitDto>> getMyUnits(Authentication authentication) {
        long startTime = System.currentTimeMillis();
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.uid();

            List<UnitDto> units = residentAccountService.getMyUnits(userId);

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 500) {
                log.warn("getMyUnits took {}ms (target: <500ms) for userId: {}", duration, userId);
            }

            return ResponseEntity.ok(units);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Failed to get my units after {}ms: {}", duration, e.getMessage());
            // Return empty list as fallback instead of error
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{residentId}/units")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<UnitDto>> getUnitsByResidentId(
            @PathVariable UUID residentId,
            Authentication authentication) {
        try {
            List<UnitDto> units = residentAccountService.getUnitsByResidentId(residentId);
            return ResponseEntity.ok(units);
        } catch (Exception e) {
            log.warn("Failed to get units for resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentDto>> getAllResidents() {
        try {
            List<ResidentDto> residents = residentService.findAll();
            return ResponseEntity.ok(residents);
        } catch (Exception e) {
            log.error("Failed to get all residents: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{residentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER', 'RESIDENT')")
    public ResponseEntity<ResidentDto> getResidentById(@PathVariable UUID residentId) {
        try {
            ResidentDto resident = residentService.getById(residentId);
            return ResponseEntity.ok(resident);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get resident {}: {}", residentId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER', 'RESIDENT')")
    public ResponseEntity<ResidentDto> getResidentByUserId(@PathVariable UUID userId) {
        try {
            ResidentDto resident = residentService.getByUserId(userId);
            return ResponseEntity.ok(resident);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get resident by user {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error getting resident by user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-national-id/{nationalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER', 'RESIDENT')")
    public ResponseEntity<ResidentDto> getResidentByNationalId(@PathVariable String nationalId) {
        try {
            ResidentDto resident = residentService.getByNationalId(nationalId);
            return ResponseEntity.ok(resident);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get resident by national ID {}: {}", nationalId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/by-phone/{phone}", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER', 'RESIDENT')")
    public ResponseEntity<Map<String, Object>> getResidentByPhone(@PathVariable("phone") String phone) {
        try {
            log.info("Getting resident by phone: {}", phone);
            ResidentDto resident = residentService.getByPhone(phone);
            log.info("Found resident: id={}, phone={}, name={}", resident.id(), resident.phone(), resident.fullName());
            // Return as Map to match what chat-service expects
            Map<String, Object> response = Map.of(
                    "id", resident.id().toString(),
                    "fullName", resident.fullName() != null ? resident.fullName() : "",
                    "phone", resident.phone() != null ? resident.phone() : "",
                    "email", resident.email() != null ? resident.email() : "");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get resident by phone {}: {}", phone, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search-by-phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER', 'RESIDENT')")
    public ResponseEntity<List<ResidentDto>> searchResidentsByPhone(
            @RequestParam("prefix") String phonePrefix) {
        try {
            log.info("🔍 [ResidentController] Searching residents by phone prefix: '{}'", phonePrefix);
            List<ResidentDto> residents = residentService.searchByPhonePrefix(phonePrefix);
            log.info("✅ [ResidentController] Found {} residents matching phone prefix: '{}'", residents.size(),
                    phonePrefix);
            if (residents.isEmpty()) {
                log.debug("⚠️ [ResidentController] No residents found. This could mean:");
                log.debug("   1. No residents have phone numbers starting with '{}'", phonePrefix);
                log.debug("   2. All matching residents have status != ACTIVE");
                log.debug("   3. Phone number format in database differs from search prefix");
            }
            return ResponseEntity.ok(residents);
        } catch (Exception e) {
            log.error("❌ [ResidentController] Failed to search residents by phone prefix '{}': {}", phonePrefix,
                    e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ResidentDto> getMyResident(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            ResidentDto resident = residentService.getByUserId(principal.uid());
            return ResponseEntity.ok(resident);
        } catch (IllegalArgumentException e) {
            log.warn("Resident not found for current user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me/basic")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ResidentDto> getMyResidentBasic(Authentication authentication) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            ResidentDto resident = residentService.getByUserId(principal.uid());
            return ResponseEntity.ok(resident);
        } catch (IllegalArgumentException e) {
            log.warn("Resident not found for current user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/staff/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResidentDto> syncStaffResident(@Valid @RequestBody StaffResidentSyncRequest request) {
        try {
            ResidentDto resident = residentService.syncStaffResident(request);
            return ResponseEntity.ok(resident);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to sync staff resident: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/check/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<Map<String, Boolean>> checkEmailExists(@RequestParam String email) {
        try {
            boolean exists = residentService.existsEmail(email, null);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking email existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exists", false));
        }
    }

    @GetMapping("/check/phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<Map<String, Boolean>> checkPhoneExists(@RequestParam String phone) {
        try {
            boolean exists = residentService.existsPhone(phone, null);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking phone existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exists", false));
        }
    }

    @GetMapping("/check/national-id")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<Map<String, Boolean>> checkNationalIdExists(@RequestParam String nationalId) {
        try {
            boolean exists = residentService.existsNationalId(nationalId, null);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking national ID existence: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("exists", false));
        }
    }

    @GetMapping("/user-ids-by-building/{buildingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<UUID>> getResidentUserIdsByBuilding(
            @PathVariable UUID buildingId,
            @RequestParam(required = false) Integer floor) {
        try {
            log.info("Fetching resident user IDs for building: {}, floor: {}", buildingId, floor);
            List<UUID> userIds = residentService.findUserIdsByBuildingIdAndFloor(buildingId, floor);
            log.info("Found {} resident user IDs for building {}, floor {}", userIds.size(), buildingId, floor);
            return ResponseEntity.ok(userIds);
        } catch (Exception e) {
            log.error("Failed to get resident user IDs by building {}, floor {}: {}", buildingId, floor, e.getMessage(),
                    e);
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/by-user-ids")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<List<ResidentDto>> getResidentsByUserIds(@RequestBody List<UUID> userIds) {
        try {
            List<ResidentDto> residents = residentService.findByUserIds(userIds);
            return ResponseEntity.ok(residents);
        } catch (Exception e) {
            log.error("Failed to get residents by user IDs: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
