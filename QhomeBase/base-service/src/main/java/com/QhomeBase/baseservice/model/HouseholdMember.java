package com.QhomeBase.baseservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "data", name = "household_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_member_unique", columnNames = {"household_id", "resident_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdMember {
    
    @Id
    @GeneratedValue
    private UUID id;
    
    @Column(name = "household_id", nullable = false)
    private UUID householdId;
    
    @Column(name = "resident_id", nullable = false)
    private UUID residentId;
    
    @Column(name = "relation")
    private String relation;
    
    @Column(name = "proof_of_relation_image_url", columnDefinition = "TEXT")
    private String proofOfRelationImageUrl;
    
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "joined_at")
    private LocalDate joinedAt;
    
    @Column(name = "left_at")
    private LocalDate leftAt;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}


