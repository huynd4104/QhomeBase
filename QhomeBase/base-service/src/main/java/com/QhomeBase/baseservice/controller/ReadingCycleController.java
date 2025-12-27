package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.ReadingCycleCreateReq;
import com.QhomeBase.baseservice.dto.ReadingCycleDto;
import com.QhomeBase.baseservice.dto.ReadingCycleUnassignedInfoDto;
import com.QhomeBase.baseservice.dto.ReadingCycleUpdateReq;
import com.QhomeBase.baseservice.model.ReadingCycleStatus;
import com.QhomeBase.baseservice.service.ReadingCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reading-cycles")
@RequiredArgsConstructor
public class ReadingCycleController {

    private final ReadingCycleService readingCycleService;

    @PostMapping
    public ResponseEntity<ReadingCycleDto> createCycle(
            @Valid @RequestBody ReadingCycleCreateReq request,
            Authentication authentication) {
        ReadingCycleDto cycle = readingCycleService.createCycle(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(cycle);
    }

    @GetMapping
    public ResponseEntity<List<ReadingCycleDto>> getAllCycles() {
        List<ReadingCycleDto> cycles = readingCycleService.getAllCycles();
        return ResponseEntity.ok(cycles);
    }

    @GetMapping("/{cycleId}")
    public ResponseEntity<ReadingCycleDto> getCycleById(@PathVariable UUID cycleId) {
        ReadingCycleDto cycle = readingCycleService.getCycleById(cycleId);
        return ResponseEntity.ok(cycle);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReadingCycleDto>> getCyclesByStatus(
            @PathVariable ReadingCycleStatus status) {
        List<ReadingCycleDto> cycles = readingCycleService.getCyclesByStatus(status);
        return ResponseEntity.ok(cycles);
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<ReadingCycleDto>> getCyclesByService(
            @PathVariable UUID serviceId) {
        List<ReadingCycleDto> cycles = readingCycleService.getCyclesByService(serviceId);
        return ResponseEntity.ok(cycles);
    }

    @GetMapping("/status/{status}/service/{serviceId}")
    public ResponseEntity<List<ReadingCycleDto>> getCyclesByStatusAndService(
            @PathVariable ReadingCycleStatus status,
            @PathVariable UUID serviceId) {
        List<ReadingCycleDto> cycles = readingCycleService.getCyclesByStatusAndService(status, serviceId);
        return ResponseEntity.ok(cycles);
    }

    @GetMapping("/{cycleId}/unassigned")
    public ResponseEntity<ReadingCycleUnassignedInfoDto> getCycleUnassignedInfo(
            @PathVariable UUID cycleId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyWithOwner) {
        ReadingCycleUnassignedInfoDto info = readingCycleService.getUnassignedUnitsInfo(cycleId, onlyWithOwner);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/period")
    public ResponseEntity<List<ReadingCycleDto>> getCyclesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<ReadingCycleDto> cycles = readingCycleService.getCyclesByPeriod(from, to);
        return ResponseEntity.ok(cycles);
    }

    @PutMapping("/{cycleId}")
    public ResponseEntity<ReadingCycleDto> updateCycle(
            @PathVariable UUID cycleId,
            @Valid @RequestBody ReadingCycleUpdateReq request,
            Authentication authentication) {
        ReadingCycleDto cycle = readingCycleService.updateCycle(cycleId, request, authentication);
        return ResponseEntity.ok(cycle);
    }

    @PatchMapping("/{cycleId}/status")
    public ResponseEntity<ReadingCycleDto> changeCycleStatus(
            @PathVariable UUID cycleId,
            @RequestParam ReadingCycleStatus status) {
        ReadingCycleDto cycle = readingCycleService.changeCycleStatus(cycleId, status);
        return ResponseEntity.ok(cycle);
    }

    @DeleteMapping("/{cycleId}")
    public ResponseEntity<Void> deleteCycle(@PathVariable UUID cycleId) {
        readingCycleService.deleteCycle(cycleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync-billing")
    public ResponseEntity<ReadingCycleService.BillingSyncResult> syncBillingCycles() {
        ReadingCycleService.BillingSyncResult result = readingCycleService.syncBillingCycles();
        return ResponseEntity.ok(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}

