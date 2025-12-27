package com.QhomeBase.customerinteractionservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema= "cs_service", name="requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Request {
    @Id @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "request_code", nullable = false)
    private String requestCode;
    @Column(name = "resident_id", nullable = false)
    private UUID residentId;
    @Column(name = "resident_name", nullable = false)
    private String residentName;
    @Column(name = "image_path", nullable = true)
    private String imagePath;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "content", nullable = false)
    private String content;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "type", nullable = true)
    private String type;
    @Column(name = "fee", nullable = true)
    private java.math.BigDecimal fee;
    @Column(name = "repaired_date", nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate repairedDate;
    @Column(name = "service_booking_id", nullable = true)
    private UUID serviceBookingId;
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
