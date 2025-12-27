package com.QhomeBase.servicescardservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "card_fee_reminder_state", schema = "card")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardFeeReminderState {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "card_type", nullable = false, length = 30)
    private String cardType; // RESIDENT, ELEVATOR, VEHICLE

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "apartment_number", length = 100)
    private String apartmentNumber;

    @Column(name = "building_name", length = 100)
    private String buildingName;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "reminder_count", nullable = false)
    @Builder.Default
    private Integer reminderCount = 0;

    @Column(name = "max_reminders", nullable = false)
    @Builder.Default
    private Integer maxReminders = 6;

    @Column(name = "last_reminded_at")
    private OffsetDateTime lastRemindedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

