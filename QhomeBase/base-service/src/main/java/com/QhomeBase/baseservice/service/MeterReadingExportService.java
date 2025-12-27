package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.BillingImportedReadingDto;
import com.QhomeBase.baseservice.dto.MeterReadingImportResponse;
import com.QhomeBase.baseservice.model.MeterReading;
import com.QhomeBase.baseservice.repository.HouseholdRepository;
import com.QhomeBase.baseservice.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingExportService {

    private final MeterReadingRepository meterReadingRepository;
    private final HouseholdRepository householdRepository;
    private final FinanceBillingClient financeBillingClient;

    @Transactional(readOnly = true)
    public MeterReadingImportResponse exportReadingsByCycle(UUID cycleId) {
        return exportReadingsByCycleAndUnit(cycleId, null);
    }
    
    @Transactional(readOnly = true)
    public MeterReadingImportResponse exportReadingsByCycleAndUnit(UUID cycleId, UUID unitId) {
        log.info("Exporting readings for cycle: {}, unitId: {}", cycleId, unitId);
        List<MeterReading> readings = meterReadingRepository.findByCycleId(cycleId);
        log.info("Found {} readings linked to cycle {} via column or assignment", readings.size(), cycleId);
        
        // Filter by unitId if provided
        if (unitId != null) {
            int beforeFilter = readings.size();
            readings = readings.stream()
                    .filter(r -> {
                        if (r == null) return false;
                        if (r.getUnit() == null) {
                            log.debug("Reading {} has no unit, skipping", r.getId());
                            return false;
                        }
                        UUID readingUnitId = r.getUnit().getId();
                        boolean matches = unitId.equals(readingUnitId);
                        if (!matches) {
                            log.debug("Reading {} unit {} does not match requested unit {}", 
                                    r.getId(), readingUnitId, unitId);
                        }
                        return matches;
                    })
                    .collect(java.util.stream.Collectors.toList());
            log.info("Filtered from {} to {} readings for unit {}", beforeFilter, readings.size(), unitId);
        }
        
        if (readings.isEmpty()) {
            String message = unitId != null 
                    ? String.format("No readings found for cycle: %s and unit: %s", cycleId, unitId)
                    : String.format("No readings found for cycle: %s", cycleId);
            log.warn(message);
            
            // If filtering by unitId, this might be expected - return empty response instead of error
            if (unitId != null) {
                log.info("No readings found for unit {} in cycle {} - this might be expected if no readings were created for this unit", unitId, cycleId);
                return MeterReadingImportResponse.builder()
                        .totalReadings(0)
                        .invoicesCreated(0)
                        .message(message)
                        .build();
            }
            
            // Only check all readings if not filtering by unitId
            List<MeterReading> allReadings = meterReadingRepository.findAll();
            log.debug("Total readings in database: {}", allReadings.size());
            
            for (MeterReading r : allReadings) {
                if (r.getAssignment() != null && r.getAssignment().getCycle() != null) {
                    log.debug("Reading {} has assignment.cycle.id = {}", r.getId(), r.getAssignment().getCycle().getId());
                } else {
                    log.debug("Reading {} has no assignment/cycle", r.getId());
                }
            }
            
            return MeterReadingImportResponse.builder()
                    .totalReadings(0)
                    .invoicesCreated(0)
                    .message(message)
                    .build();
        }
        
        long mismatchedCycleCount = readings.stream()
                .filter(r -> r.getAssignment() != null && r.getAssignment().getCycle() != null)
                .filter(r -> !cycleId.equals(r.getAssignment().getCycle().getId()))
                .count();
        if (mismatchedCycleCount > 0) {
            log.warn("Detected {} readings whose assignment.cycle != requested cycle {}", mismatchedCycleCount, cycleId);
            readings.stream()
                    .filter(r -> r.getAssignment() != null && r.getAssignment().getCycle() != null)
                    .filter(r -> !cycleId.equals(r.getAssignment().getCycle().getId()))
                    .limit(20)
                    .forEach(r -> log.warn("Reading {} -> assignment {} cycle {}", r.getId(),
                            r.getAssignment().getId(), r.getAssignment().getCycle().getId()));
        }
        log.info("Proceeding with {} readings for cycle {}", readings.size(), cycleId);

        List<BillingImportedReadingDto> billingReadings = convertToBillingReadings(readings);
        MeterReadingImportResponse response = financeBillingClient.importMeterReadingsSync(billingReadings);
        
        log.info("Exported {} readings from cycle {} to finance-billing. Invoices created: {}", 
                readings.size(), cycleId, response != null ? response.getInvoicesCreated() : 0);
        return response;
    }

    private List<BillingImportedReadingDto> convertToBillingReadings(List<MeterReading> readings) {
        List<BillingImportedReadingDto> result = new ArrayList<>();
        
        for (MeterReading reading : readings) {
            if (reading.getMeter() == null || reading.getUnit() == null) {
                log.warn("Skipping reading {} - missing meter or unit", reading.getId());
                continue;
            }

            UUID unitId = reading.getUnit().getId();
            UUID residentId = getResidentId(unitId);
            
            if (residentId == null) {
                log.warn("No active resident found for unit {}, but proceeding with null residentId for reading {}", unitId, reading.getId());
            }

            UUID cycleId = null;
            // Try to get cycleId from assignment first, then from reading's cycleId column
            if (reading.getAssignment() != null && reading.getAssignment().getCycle() != null) {
                cycleId = reading.getAssignment().getCycle().getId();
            } else if (reading.getCycleId() != null) {
                // Fallback to reading's cycleId column if assignment is missing
                cycleId = reading.getCycleId();
                log.debug("Reading {} has no assignment, using cycleId from column: {}", reading.getId(), cycleId);
            } else {
                log.warn("Skipping reading {} - missing assignment/cycle and cycleId column", reading.getId());
                continue;
            }
            if (!cycleId.equals(reading.getCycleId())) {
                log.warn("Reading {} cycle mismatch: column={} assignment.cycle={}", reading.getId(),
                        reading.getCycleId(), cycleId);
            }
            log.debug("Preparing billing reading {} unit {} assignment {} cycle {}", reading.getId(),
                    unitId, reading.getAssignment() != null ? reading.getAssignment().getId() : null, cycleId);
            String serviceCode = reading.getMeter().getService() != null 
                    ? reading.getMeter().getService().getCode() 
                    : null;

            if (serviceCode == null) {
                log.warn("Skipping reading {} - missing service code", reading.getId());
                continue;
            }

            BigDecimal usageKwh = reading.getCurrIndex() != null && reading.getPrevIndex() != null
                    ? reading.getCurrIndex().subtract(reading.getPrevIndex())
                    : null;

            if (usageKwh == null || usageKwh.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Skipping reading {} - invalid usage: {}", reading.getId(), usageKwh);
                continue;
            }

            BillingImportedReadingDto billingReading = BillingImportedReadingDto.builder()
                    .unitId(unitId)
                    .residentId(residentId)
                    .cycleId(cycleId)
                    .readingDate(reading.getReadingDate())
                    .usageKwh(usageKwh)
                    .serviceCode(serviceCode)
                    .description(buildDescription(reading))
                    .externalReadingId(reading.getId())
                    .build();

            result.add(billingReading);
        }

        return result;
    }

    private UUID getResidentId(UUID unitId) {
        return householdRepository.findCurrentHouseholdByUnitId(unitId)
                .map(household -> household.getPrimaryResidentId())
                .orElse(null);
    }

    private String buildDescription(MeterReading reading) {
        StringBuilder desc = new StringBuilder();
        if (reading.getMeter() != null && reading.getMeter().getMeterCode() != null) {
            desc.append("Meter: ").append(reading.getMeter().getMeterCode());
        }
        if (reading.getNote() != null && !reading.getNote().trim().isEmpty()) {
            if (desc.length() > 0) desc.append(" - ");
            desc.append(reading.getNote());
        }
        return desc.length() > 0 ? desc.toString() : null;
    }
}
