package com.QhomeBase.baseservice.scheduler;

import com.QhomeBase.baseservice.service.MeterReadingReminderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class MeterReadingReminderScheduler {

    private final MeterReadingReminderService reminderService;

    @Value("${meter-reading.reminder.timezone:Asia/Ho_Chi_Minh}")
    private String timezoneId;

    @PostConstruct
    public void initializeReminders() {
        runReminderJob(LocalDate.now(resolveZoneId()));
    }

    @Scheduled(cron = "${meter-reading.reminder.cron:0 0 8 * * *}")
    public void scheduledReminderJob() {
        runReminderJob(LocalDate.now(resolveZoneId()));
    }

    private void runReminderJob(LocalDate today) {
        try {
            reminderService.processReminders(today);
            log.debug("[MeterReminder] Processed reminders for {}", today);
        } catch (Exception ex) {
            log.error("[MeterReminder] Reminder job failed", ex);
        }
    }

    private ZoneId resolveZoneId() {
        try {
            return ZoneId.of(timezoneId);
        } catch (Exception ex) {
            log.warn("[MeterReminder] Invalid timezone '{}', fallback to system default", timezoneId);
            return ZoneId.systemDefault();
        }
    }
}

