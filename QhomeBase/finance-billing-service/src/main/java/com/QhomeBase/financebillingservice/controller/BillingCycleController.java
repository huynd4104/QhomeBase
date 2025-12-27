package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.BillingCycleDto;
import com.QhomeBase.financebillingservice.dto.CreateBillingCycleRequest;
import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import com.QhomeBase.financebillingservice.service.BillingCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing-cycles")
@RequiredArgsConstructor
public class BillingCycleController {

    private final BillingCycleService billingCycleService;

    @GetMapping("/loadPeriod")
    public List<BillingCycleDto> findByTenantIdAndYear(@RequestParam Integer year) {
        return billingCycleService.loadPeriod(year);
    }

    @PostMapping("/sync-missing")
    public List<BillingCycleDto> createMissingBillingCycles() {
        return billingCycleService.createBillingCyclesForMissingReadingCycles();
    }

    @GetMapping("/missing")
    public List<ReadingCycleDto> getMissingReadingCycles() {
        return billingCycleService.getMissingReadingCyclesInfo();
    }

    @GetMapping
    public List<BillingCycleDto> getBillingCycle(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return billingCycleService.getListByTime(startDate, endDate);
    }

    @GetMapping("/external/{externalCycleId}")
    public List<BillingCycleDto> getByExternalCycleId(@PathVariable UUID externalCycleId) {
        return billingCycleService.findByExternalCycleId(externalCycleId);
    }

    @PostMapping
    public ResponseEntity<BillingCycleDto> createBillingCycle(@RequestBody CreateBillingCycleRequest request) {
        BillingCycleDto cycle = billingCycleService.createBillingCycle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cycle);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BillingCycleDto> updateBillingCycleStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        BillingCycleDto cycle = billingCycleService.updateBillingCycleStatus(id, status);
        return ResponseEntity.ok(cycle);
    }
}