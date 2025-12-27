package com.QhomeBase.customerinteractionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema= "cs_service", name="processing_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProcessingLog {
    @Id @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "record_id", nullable = false)
    private UUID recordId;
    @Column(name = "staff_in_charge", nullable = true)
    private UUID staffInCharge;
    @Column(name = "content", nullable = true)
    private String content;
    @Column(name = "request_status", nullable = true)
    private String requestStatus;
    @Column(name = "staff_in_charge_name", nullable = true)
    private String staffInChargeName;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
