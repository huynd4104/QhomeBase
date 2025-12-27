package com.QhomeBase.servicescardservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "register_vehicle", schema = "card")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "request_type")
    @Builder.Default
    private String requestType = "NEW_CARD";

    @Column(name = "note")
    private String note;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "admin_note")
    private String adminNote;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "vehicle_brand")
    private String vehicleBrand;

    @Column(name = "vehicle_color")
    private String vehicleColor;

    @Column(name = "apartment_number")
    private String apartmentNumber;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "payment_status")
    @Builder.Default
    private String paymentStatus = "UNPAID";

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "payment_amount", precision = 14, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "payment_date")
    private OffsetDateTime paymentDate;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "vnpay_transaction_ref")
    private String vnpayTransactionRef;

    @Column(name = "vnpay_initiated_at")
    private OffsetDateTime vnpayInitiatedAt;


    @Column(name = "reissued_from_card_id")
    private UUID reissuedFromCardId;

    @OneToMany(mappedBy = "registerServiceRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RegisterServiceImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public void addImage(RegisterServiceImage image) {
        images.add(image);
        image.setRegisterServiceRequest(this);
    }

    public void removeImage(RegisterServiceImage image) {
        images.remove(image);
        image.setRegisterServiceRequest(null);
    }
}

