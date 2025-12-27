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
@Table(name = "group_members", schema = "chat_service", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "resident_id"}),
       indexes = {
           @Index(name = "idx_group_members_group_id", columnList = "group_id"),
           @Index(name = "idx_group_members_resident_id", columnList = "resident_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "group_id", insertable = false, updatable = false)
    private UUID groupId;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private String role = "MEMBER"; // ADMIN, MODERATOR, MEMBER

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "last_read_at")
    private OffsetDateTime lastReadAt;

    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private Boolean isMuted = false;

    @Column(name = "mute_until")
    private OffsetDateTime muteUntil; // Timestamp when mute expires (null = not muted or muted indefinitely)

    @Column(name = "muted_by_user_id")
    private UUID mutedByUserId; // User who set the mute

    @Column(name = "hidden_at")
    private OffsetDateTime hiddenAt; // Timestamp when user hid/deleted the group. NULL means visible.
}

