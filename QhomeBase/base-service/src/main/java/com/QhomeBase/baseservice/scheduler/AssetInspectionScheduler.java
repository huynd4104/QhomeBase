package com.QhomeBase.baseservice.scheduler;

import com.QhomeBase.baseservice.service.AssetInspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssetInspectionScheduler {

    private final AssetInspectionService assetInspectionService;
    private final ZoneId zoneId = ZoneId.systemDefault();

    @Scheduled(cron = "${asset-inspection.scheduler.cron:0 0 0 28 * *}")
    public void createInspectionsForExpiredContracts() {
        try {
            log.info("Starting scheduled job: Create inspections for expired contracts");
            
            YearMonth currentMonth = YearMonth.now(zoneId);
            LocalDate endOfMonth = currentMonth.atEndOfMonth();
            
            log.info("Creating inspections for contracts expired in month: {}", currentMonth);
            int createdCount = assetInspectionService.createInspectionsForExpiredContracts(endOfMonth);
            
            log.info("Completed scheduled job: Created {} inspections for expired contracts", createdCount);
        } catch (Exception e) {
            log.error("Error in scheduled job to create inspections for expired contracts", e);
        }
    }
}

