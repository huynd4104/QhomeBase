package com.QhomeBase.chatservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "blocks", schema = "chat_service", indexes = {
    @Index(name = "idx_blocks_blocker_id", columnList = "blocker_id"),
    @Index(name = "idx_blocks_blocked_id", columnList = "blocked_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId; // Resident who blocks

    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId; // Resident who is blocked

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

