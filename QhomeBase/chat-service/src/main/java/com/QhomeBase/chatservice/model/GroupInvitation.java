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
@Table(name = "group_invitations", schema = "chat_service",
       indexes = {
           @Index(name = "idx_group_invitations_group_id", columnList = "group_id"),
           @Index(name = "idx_group_invitations_invitee_phone", columnList = "invitee_phone"),
           @Index(name = "idx_group_invitations_invitee_resident_id", columnList = "invitee_resident_id"),
           @Index(name = "idx_group_invitations_status", columnList = "status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private UUID groupId;

    @Column(name = "inviter_id", nullable = false)
    private UUID inviterId;

    @Column(name = "invitee_phone", nullable = false, length = 20)
    private String inviteePhone;

    @Column(name = "invitee_resident_id")
    private UUID inviteeResidentId;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, DECLINED (no longer EXPIRED - invitations don't expire)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt; // No longer used - invitations don't expire, only accept/decline changes status

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;
}

