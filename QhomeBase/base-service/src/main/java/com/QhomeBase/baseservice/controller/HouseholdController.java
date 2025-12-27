package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.HouseholdCreateDto;
import com.QhomeBase.baseservice.dto.HouseholdDto;
import com.QhomeBase.baseservice.dto.HouseholdUpdateDto;
import com.QhomeBase.baseservice.service.HouseholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/households")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
@Slf4j
public class HouseholdController {

    private final HouseholdService householdService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createHousehold(@Valid @RequestBody HouseholdCreateDto createDto) {
        try {
            HouseholdDto result = householdService.createHousehold(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create household: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateHousehold(
            @PathVariable UUID id,
            @Valid @RequestBody HouseholdUpdateDto updateDto) {
        try {
            HouseholdDto result = householdService.updateHousehold(id, updateDto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update household {}: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHousehold(@PathVariable UUID id) {
        try {
            householdService.deleteHousehold(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete household {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<HouseholdDto> getHouseholdById(@PathVariable UUID id) {
        try {
            HouseholdDto result = householdService.getHouseholdById(id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get household {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/units/{unitId}/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<HouseholdDto> getCurrentHouseholdByUnitId(@PathVariable UUID unitId) {
        try {
            HouseholdDto result = householdService.getCurrentHouseholdByUnitId(unitId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // Unit has no active household - return 404, no logging needed
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Only log stacktrace for >=500 errors (production-ready)
            log.error("Error getting current household for unit {}: {}", unitId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/units/{unitId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<List<HouseholdDto>> getAllHouseholdsByUnitId(@PathVariable UUID unitId) {
        try {
            List<HouseholdDto> result = householdService.getAllHouseholdsByUnitId(unitId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("Failed to get households for unit {}: {}", unitId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private record ErrorResponse(String message) {}
}

