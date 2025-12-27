package com.QhomeBase.servicescardservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "register_vehicle_image", schema = "card")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceImage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "register_vehicle_id", nullable = false)
    private RegisterServiceRequest registerServiceRequest;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

