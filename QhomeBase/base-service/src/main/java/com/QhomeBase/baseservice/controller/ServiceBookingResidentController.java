package com.QhomeBase.baseservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-booking")
@RequiredArgsConstructor
@Slf4j
public class ServiceBookingResidentController {

    @GetMapping("/unpaid")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<List<Map<String, Object>>> getUnpaidBookings(Authentication authentication) {
        log.info("Resident {} requested unpaid service bookings, returning empty list (placeholder)",
                authentication != null ? authentication.getName() : "anonymous");
        return ResponseEntity.ok(Collections.emptyList());
    }
}

