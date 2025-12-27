package com.QhomeBase.assetmaintenanceservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "asset_suppliers", schema = "asset")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "relationship_type", nullable = false)
    @Builder.Default
    private String relationshipType = "PURCHASE"; // Chỉ hỗ trợ PURCHASE. Warranty và maintenance service do nội bộ xử lý, không phải từ external supplier.

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "warranty_provider")
    private String warrantyProvider; // Lưu thông tin warranty từ supplier (nếu có), nhưng warranty service do nội bộ xử lý

    @Column(name = "warranty_contact")
    private String warrantyContact; // Contact từ supplier cho warranty claim (nếu cần), nhưng thực tế maintenance do nội bộ làm

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "created_by", nullable = false)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by")
    private String updatedBy;

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

