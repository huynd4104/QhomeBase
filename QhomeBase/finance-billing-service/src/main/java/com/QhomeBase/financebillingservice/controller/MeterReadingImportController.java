package com.QhomeBase.financebillingservice.controller;

import com.QhomeBase.financebillingservice.dto.ImportedReadingDto;
import com.QhomeBase.financebillingservice.dto.MeterReadingImportResponse;
import com.QhomeBase.financebillingservice.service.MeterReadingImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meter-readings")
@RequiredArgsConstructor
@Slf4j
public class MeterReadingImportController {

    private final MeterReadingImportService importService;

    @PostMapping("/import")
    public ResponseEntity<MeterReadingImportResponse> importReadings(@RequestBody List<ImportedReadingDto> readings) {
        try {
            int count = readings != null ? readings.size() : 0;
            log.info("Received {} imported readings for invoicing", count);
            
            if (readings == null || readings.isEmpty()) {
                log.warn("Received empty or null readings list");
                return ResponseEntity.badRequest()
                        .body(MeterReadingImportResponse.builder()
                                .totalReadings(0)
                                .invoicesCreated(0)
                                .message("No readings provided")
                                .build());
            }
            
            MeterReadingImportResponse response = importService.importReadingsWithResponse(readings);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Bad request when importing readings: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(MeterReadingImportResponse.builder()
                            .totalReadings(0)
                            .invoicesCreated(0)
                            .message("Bad request: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error when importing readings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MeterReadingImportResponse.builder()
                            .totalReadings(0)
                            .invoicesCreated(0)
                            .message("Internal server error: " + e.getMessage())
                            .build());
        }
    }
}


