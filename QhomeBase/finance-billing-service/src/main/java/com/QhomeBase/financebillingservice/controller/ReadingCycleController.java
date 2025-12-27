package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.client.BaseServiceClient;
import com.QhomeBase.financebillingservice.dto.ReadingCycleDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reading-cycles")
@RequiredArgsConstructor
public class ReadingCycleController {

    private static final Logger log = LoggerFactory.getLogger(ReadingCycleController.class);

    private final BaseServiceClient baseServiceClient;

    @GetMapping
    public ResponseEntity<List<ReadingCycleDto>> getAllReadingCycles() {
        log.info("Forwarding request to base-service to load reading cycles");
        List<ReadingCycleDto> cycles = baseServiceClient.getAllReadingCycles();
        return ResponseEntity.ok(cycles);
    }
}

