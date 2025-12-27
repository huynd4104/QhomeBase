package com.QhomeBase.baseservice.model;

import com.QhomeBase.baseservice.util.StringListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cleaning_requests", schema = "data")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleaningRequest {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "resident_id")
    private UUID residentId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "cleaning_type", nullable = false)
    private String cleaningType;

    @Column(name = "cleaning_date", nullable = false)
    private LocalDate cleaningDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_hours", nullable = false)
    private BigDecimal durationHours;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "note")
    private String note;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "user_id")
    private UUID userId;

    @Convert(converter = StringListConverter.class)
    @Column(name = "extra_services", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> extraServices = new ArrayList<>();

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_resent_at")
    private OffsetDateTime lastResentAt;

    @Column(name = "resend_alert_sent", nullable = false)
    @Builder.Default
    private boolean resendAlertSent = false;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

