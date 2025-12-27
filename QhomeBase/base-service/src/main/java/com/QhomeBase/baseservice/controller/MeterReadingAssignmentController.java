package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.MeterDto;
import com.QhomeBase.baseservice.dto.MeterReadingAssignmentCreateReq;
import com.QhomeBase.baseservice.dto.MeterReadingAssignmentDto;
import com.QhomeBase.baseservice.dto.AssignmentProgressDto;
import com.QhomeBase.baseservice.dto.MeterWithReadingDto;
import com.QhomeBase.baseservice.security.UserPrincipal;
import com.QhomeBase.baseservice.service.MeterReadingAssignmentService;
import com.QhomeBase.baseservice.service.MeterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meter-reading-assignments")
@RequiredArgsConstructor
public class MeterReadingAssignmentController {

    private final MeterReadingAssignmentService assignmentService;
    private final MeterService meterService;

    @PostMapping
    public ResponseEntity<MeterReadingAssignmentDto> createAssignment(
            @Valid @RequestBody MeterReadingAssignmentCreateReq request,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        MeterReadingAssignmentDto assignment = assignmentService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @GetMapping("/{assignmentId}")
    public ResponseEntity<MeterReadingAssignmentDto> getAssignmentById(
            @PathVariable UUID assignmentId) {
        MeterReadingAssignmentDto assignment = assignmentService.getById(assignmentId);
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/cycle/{cycleId}")
    public ResponseEntity<List<MeterReadingAssignmentDto>> getAssignmentsByCycle(
            @PathVariable UUID cycleId) {
        List<MeterReadingAssignmentDto> assignments = assignmentService.getByCycleId(cycleId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<MeterReadingAssignmentDto>> getAssignmentsByStaff(
            @PathVariable UUID staffId) {
        List<MeterReadingAssignmentDto> assignments = assignmentService.getByAssignedTo(staffId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/staff/{staffId}/active")
    public ResponseEntity<List<MeterReadingAssignmentDto>> getActiveAssignmentsByStaff(
            @PathVariable UUID staffId) {
        List<MeterReadingAssignmentDto> assignments = assignmentService.getActiveByStaff(staffId);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/my-assignments")
    public ResponseEntity<List<MeterReadingAssignmentDto>> getMyAssignments(
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<MeterReadingAssignmentDto> assignments = assignmentService.getByAssignedTo(principal.uid());
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/my-assignments/active")
    public ResponseEntity<List<MeterReadingAssignmentDto>> getMyActiveAssignments(
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<MeterReadingAssignmentDto> assignments = assignmentService.getActiveByStaff(principal.uid());
        return ResponseEntity.ok(assignments);
    }

    @PatchMapping("/{assignmentId}/complete")
    public ResponseEntity<MeterReadingAssignmentDto> completeAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        MeterReadingAssignmentDto assignment = assignmentService.markAsCompleted(assignmentId, principal);
        return ResponseEntity.ok(assignment);
    }

    @PatchMapping("/{assignmentId}/cancel")
    public ResponseEntity<MeterReadingAssignmentDto> cancelAssignment(
            @PathVariable UUID assignmentId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        MeterReadingAssignmentDto assignment = assignmentService.cancelAssignment(assignmentId, principal);
        return ResponseEntity.ok(assignment);
    }

    @GetMapping("/{assignmentId}/meters")
    public ResponseEntity<List<MeterDto>> getMetersByAssignment(@PathVariable UUID assignmentId) {
        List<MeterDto> meters = meterService.getMetersByAssignment(assignmentId);
        return ResponseEntity.ok(meters);
    }

    @GetMapping("/{assignmentId}/progress")
    public ResponseEntity<AssignmentProgressDto> getProgress(@PathVariable UUID assignmentId) {
        AssignmentProgressDto progress = assignmentService.getProgress(assignmentId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/staff/{staffId}/cycle/{cycleId}/meters")
    public ResponseEntity<List<MeterDto>> getMetersByStaffAndCycle(
            @PathVariable UUID staffId,
            @PathVariable UUID cycleId) {
        List<MeterDto> meters = meterService.getMetersByStaffAndCycle(staffId, cycleId);
        return ResponseEntity.ok(meters);
    }

    @GetMapping("/staff/{staffId}/cycle/{cycleId}/meters-with-reading")
    public ResponseEntity<List<MeterWithReadingDto>> getMetersWithReadingByStaffAndCycle(
            @PathVariable UUID staffId,
            @PathVariable UUID cycleId) {
        List<MeterWithReadingDto> meters = meterService.getMetersWithReadingByStaffAndCycle(staffId, cycleId);
        return ResponseEntity.ok(meters);
    }

    @GetMapping("/my-meters/cycle/{cycleId}")
    public ResponseEntity<List<MeterDto>> getMyMetersByCycle(
            @PathVariable UUID cycleId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<MeterDto> meters = meterService.getMetersByStaffAndCycle(principal.uid(), cycleId);
        return ResponseEntity.ok(meters);
    }

    @GetMapping("/my-meters/cycle/{cycleId}/with-reading")
    public ResponseEntity<List<MeterWithReadingDto>> getMyMetersWithReadingByCycle(
            @PathVariable UUID cycleId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<MeterWithReadingDto> meters = meterService.getMetersWithReadingByStaffAndCycle(principal.uid(), cycleId);
        return ResponseEntity.ok(meters);
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable UUID assignmentId) {
        assignmentService.delete(assignmentId);
        return ResponseEntity.noContent().build();
    }
}

