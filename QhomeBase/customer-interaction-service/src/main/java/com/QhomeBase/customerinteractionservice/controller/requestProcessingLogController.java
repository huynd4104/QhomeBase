package com.QhomeBase.customerinteractionservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.QhomeBase.customerinteractionservice.dto.ProcessingLogDTO;
import com.QhomeBase.customerinteractionservice.service.processingLogService;
import org.springframework.security.core.Authentication;

@CrossOrigin(origins = "http://localhost:3000")

@RestController
@RequestMapping("/api/customer-interaction/requests-logs")
public class requestProcessingLogController {
    
    private final processingLogService processingLogService;
    
    public requestProcessingLogController(processingLogService processingLogService) {
        this.processingLogService = processingLogService;
    }

    @GetMapping("/{id}")
    public List<ProcessingLogDTO> getProcessingLog(@PathVariable("id") UUID id) {
        return processingLogService.getProcessingLogsById(id);
    }

    @GetMapping("/{id}/logs")
    public List<ProcessingLogDTO> getProcessingLogsByLogsId(@PathVariable("id") UUID id) {
        return processingLogService.getProcessingLogsById(id);
    }

    @GetMapping("/staff/{staffId}")
    public List<ProcessingLogDTO> getProcessingLogsByStaffId(@PathVariable("staffId") UUID staffId) {
        return processingLogService.getProcessingLogsByStaffId(staffId);
    }

    @PostMapping("/{requestId}/logs")
    public ResponseEntity<ProcessingLogDTO> addNewProcessLog(
            @PathVariable("requestId") UUID id,
            @RequestBody ProcessingLogDTO logData,
            Authentication authentication) {

        ProcessingLogDTO newLog = processingLogService.addProcessingLog(id, logData, authentication);
        return ResponseEntity.ok(newLog);
    }

}
