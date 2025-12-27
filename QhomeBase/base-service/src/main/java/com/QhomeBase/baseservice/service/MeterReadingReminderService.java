package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.MeterReadingReminderDto;
import com.QhomeBase.baseservice.model.MeterReadingAssignment;
import com.QhomeBase.baseservice.model.MeterReadingAssignmentStatus;
import com.QhomeBase.baseservice.model.MeterReadingReminder;
import com.QhomeBase.baseservice.repository.MeterReadingAssignmentRepository;
import com.QhomeBase.baseservice.repository.MeterReadingReminderRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeterReadingReminderService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String REMINDER_TYPE = "METER_READING_ASSIGNMENT_REMINDER";

    private final MeterReadingAssignmentRepository assignmentRepository;
    private final MeterReadingReminderRepository reminderRepository;

    @Value("${meter-reading.reminder.days-before:3}")
    private int reminderDaysBefore;

    @Transactional
    public void processReminders(LocalDate today) {
        LocalDate threshold = today.plusDays(Math.max(reminderDaysBefore, 0));

        Set<MeterReadingAssignmentStatus> statuses = EnumSet.of(
                MeterReadingAssignmentStatus.PENDING,
                MeterReadingAssignmentStatus.IN_PROGRESS,
                MeterReadingAssignmentStatus.OVERDUE
        );

        List<MeterReadingAssignment> assignments = assignmentRepository.findAssignmentsNeedingReminder(
                List.copyOf(statuses),
                today,
                threshold,
                today
        );

        if (assignments.isEmpty()) {
            log.debug("[MeterReminder] No assignments need reminders for {}", today);
            return;
        }

        log.info("[MeterReminder] Found {} assignments needing reminders", assignments.size());

        assignments.forEach(assignment -> {
            try {
                LocalDate dueDate = assignment.getEndDate();
                if (dueDate == null) {
                    log.warn("[MeterReminder] Assignment {} has no endDate, skipping", assignment.getId());
                    return;
                }
                
                long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
                log.debug("[MeterReminder] Processing assignment {}: due in {} days (dueDate: {})", 
                        assignment.getId(), daysUntilDue, dueDate);
                
                sendReminder(assignment);
                assignment.setReminderLastSentDate(today);
                assignmentRepository.save(assignment);
            } catch (Exception ex) {
                log.error("[MeterReminder] Failed to send reminder for assignment {}", assignment.getId(), ex);
            }
        });
    }

    private void sendReminder(MeterReadingAssignment assignment) {
        LocalDate dueDate = assignment.getEndDate();
        if (dueDate == null) {
            log.warn("[MeterReminder] Assignment {} has no endDate, skipping reminder creation", assignment.getId());
            return;
        }

        List<MeterReadingReminder> existingReminders = reminderRepository.findByAssignment_Id(assignment.getId());
        if (!existingReminders.isEmpty()) {
            MeterReadingReminder latestReminder = existingReminders.stream()
                    .max((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                    .orElse(null);
            
            if (latestReminder != null && latestReminder.getAcknowledgedAt() == null) {
                log.debug("[MeterReminder] Assignment {} already has unacknowledged reminder {}, skipping", 
                        assignment.getId(), latestReminder.getId());
                return;
            }
        }

        String cycleName = assignment.getCycle() != null ? assignment.getCycle().getName() : "chu kỳ";
        String buildingName = assignment.getBuilding() != null ? assignment.getBuilding().getName() : null;

        String title = "Nhắc đo chỉ số " + cycleName;
        StringBuilder body = new StringBuilder("Hạn đọc đến ngày ")
                .append(dueDate.format(DATE_FORMATTER));

        if (buildingName != null) {
            body.append(" tại ").append(buildingName);
        }

        MeterReadingReminder reminder = MeterReadingReminder.builder()
                .assignment(assignment)
                .userId(assignment.getAssignedTo())
                .title(title)
                .message(body.toString())
                .dueDate(dueDate)
                .type(REMINDER_TYPE)
                .build();

        reminderRepository.save(reminder);

        log.info("[MeterReminder] Created reminder {} for user {} (assignment {}, dueDate: {})", 
                reminder.getId(), assignment.getAssignedTo(), assignment.getId(), dueDate);
    }

    @Transactional
    public List<MeterReadingReminderDto> getReminders(Authentication authentication, boolean includeAcknowledged) {
        UUID userId = ((UserPrincipal) authentication.getPrincipal()).uid();

        List<MeterReadingReminder> reminders = includeAcknowledged
                ? reminderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : reminderRepository.findByUserIdAndAcknowledgedAtIsNullOrderByCreatedAtDesc(userId);

        return reminders.stream().map(this::toDto).toList();
    }

    @Transactional
    public void acknowledgeReminder(UUID reminderId, Authentication authentication) {
        UUID userId = ((UserPrincipal) authentication.getPrincipal()).uid();

        MeterReadingReminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));

        if (reminder.getAcknowledgedAt() == null) {
            reminder.setAcknowledgedAt(OffsetDateTime.now());
            reminderRepository.save(reminder);
        }
    }

    public Object debugAssignmentsNeedingReminders() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(Math.max(reminderDaysBefore, 0));

        Set<MeterReadingAssignmentStatus> statuses = EnumSet.of(
                MeterReadingAssignmentStatus.PENDING,
                MeterReadingAssignmentStatus.IN_PROGRESS,
                MeterReadingAssignmentStatus.OVERDUE
        );

        List<MeterReadingAssignment> allAssignments = assignmentRepository.findAll();
        
        java.util.Map<String, Object> debug = new java.util.HashMap<>();
        debug.put("today", today.toString());
        debug.put("threshold", threshold.toString());
        debug.put("reminderDaysBefore", reminderDaysBefore);
        debug.put("targetStatuses", statuses.stream().map(Enum::name).toList());
        
        List<java.util.Map<String, Object>> assignmentDebug = new java.util.ArrayList<>();
        for (MeterReadingAssignment a : allAssignments) {
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", a.getId().toString());
            info.put("status", a.getStatus().name());
            info.put("endDate", a.getEndDate() != null ? a.getEndDate().toString() : null);
            info.put("reminderLastSentDate", a.getReminderLastSentDate() != null ? a.getReminderLastSentDate().toString() : null);
            info.put("assignedTo", a.getAssignedTo().toString());
            
            boolean matchesStatus = statuses.contains(a.getStatus());
            boolean hasEndDate = a.getEndDate() != null;
            boolean endDateInRange = hasEndDate && !a.getEndDate().isBefore(today) && !a.getEndDate().isAfter(threshold);
            boolean reminderNotSentToday = a.getReminderLastSentDate() == null || a.getReminderLastSentDate().isBefore(today);
            
            info.put("matchesStatus", matchesStatus);
            info.put("hasEndDate", hasEndDate);
            info.put("endDateInRange", endDateInRange);
            info.put("reminderNotSentToday", reminderNotSentToday);
            info.put("shouldGetReminder", matchesStatus && hasEndDate && endDateInRange && reminderNotSentToday);
            
            if (hasEndDate) {
                long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, a.getEndDate());
                info.put("daysUntilDue", daysUntilDue);
            }
            
            assignmentDebug.add(info);
        }
        debug.put("assignments", assignmentDebug);
        
        List<MeterReadingAssignment> matchingAssignments = assignmentRepository.findAssignmentsNeedingReminder(
                List.copyOf(statuses),
                today,
                threshold,
                today
        );
        debug.put("matchingAssignmentsCount", matchingAssignments.size());
        debug.put("matchingAssignmentIds", matchingAssignments.stream().map(a -> a.getId().toString()).toList());
        
        return debug;
    }

    private MeterReadingReminderDto toDto(MeterReadingReminder reminder) {
        MeterReadingAssignment assignment = reminder.getAssignment();
        return new MeterReadingReminderDto(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getMessage(),
                reminder.getDueDate(),
                reminder.getCreatedAt(),
                reminder.getAcknowledgedAt(),
                assignment != null ? assignment.getId() : null,
                assignment != null && assignment.getCycle() != null ? assignment.getCycle().getId() : null,
                assignment != null && assignment.getCycle() != null ? assignment.getCycle().getName() : null,
                assignment != null && assignment.getBuilding() != null ? assignment.getBuilding().getId() : null,
                reminder.getType()
        );
    }
}

