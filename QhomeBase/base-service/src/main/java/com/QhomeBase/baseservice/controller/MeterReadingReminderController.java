package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.MeterReadingReminderDto;
import com.QhomeBase.baseservice.service.MeterReadingReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meter-reading/reminders")
@RequiredArgsConstructor
public class MeterReadingReminderController {

    private final MeterReadingReminderService reminderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MeterReadingReminderDto>> getMyReminders(
            Authentication authentication,
            @RequestParam(name = "includeAcknowledged", defaultValue = "false") boolean includeAcknowledged
    ) {
        return ResponseEntity.ok(reminderService.getReminders(authentication, includeAcknowledged));
    }

    // Acknowledge endpoint removed per requirement

    @PostMapping("/trigger")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> triggerReminders() {
        reminderService.processReminders(java.time.LocalDate.now());
        return ResponseEntity.ok("Reminders processed successfully");
    }

    @GetMapping("/debug/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORTER')")
    public ResponseEntity<Object> debugAssignments() {
        return ResponseEntity.ok(reminderService.debugAssignmentsNeedingReminders());
    }
}

