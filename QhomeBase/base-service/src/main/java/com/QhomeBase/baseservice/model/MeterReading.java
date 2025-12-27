package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "meter_readings", schema = "data",
       uniqueConstraints = @UniqueConstraint(name = "uq_meter_reading_meter_date_cycle", columnNames = {"meter_id", "reading_date", "cycle_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReading {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meter_id", nullable = false)
    private Meter meter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private MeterReadingAssignment assignment;

    @Column(name = "cycle_id")
    private UUID cycleId;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "prev_index", nullable = false, precision = 14, scale = 3)
    private BigDecimal prevIndex;

    @Column(name = "curr_index", nullable = false, precision = 14, scale = 3)
    private BigDecimal currIndex;

    @Column(name = "consumption", precision = 14, scale = 3, insertable = false, updatable = false)
    private BigDecimal consumption;

    @Column(name = "photo_file_id")
    private UUID photoFileId;

    @Column(name = "note")
    private String note;

    @Column(name = "reader_id", nullable = false)
    private UUID readerId;

    @Column(name = "read_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime readAt = OffsetDateTime.now();

    @Column(name = "verified")
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "disputed")
    @Builder.Default
    private Boolean disputed = false;

    @Column(name = "disputed_by")
    private UUID disputedBy;

    @Column(name = "disputed_at")
    private OffsetDateTime disputedAt;

    @Column(name = "dispute_reason")
    private String disputeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

