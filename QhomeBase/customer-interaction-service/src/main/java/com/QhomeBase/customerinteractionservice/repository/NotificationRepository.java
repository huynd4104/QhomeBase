package com.QhomeBase.customerinteractionservice.repository;

import com.QhomeBase.customerinteractionservice.model.Notification;
import com.QhomeBase.customerinteractionservice.model.NotificationScope;
import com.QhomeBase.customerinteractionservice.model.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND (:targetRole IS NULL OR n.targetRole = :targetRole)
        AND (:targetBuildingId IS NULL OR n.targetBuildingId = :targetBuildingId)
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeAndTarget(
            @Param("scope") NotificationScope scope,
            @Param("targetRole") String targetRole,
            @Param("targetBuildingId") UUID targetBuildingId
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.targetBuildingId = :targetBuildingId
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeAndBuildingId(
            @Param("scope") NotificationScope scope,
            @Param("targetBuildingId") UUID targetBuildingId
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.targetRole = :targetRole
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeAndRole(
            @Param("scope") NotificationScope scope,
            @Param("targetRole") String targetRole
    );

    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByScopeOrderByCreatedAtDesc(NotificationScope scope);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findAllActive();
    
    /**
     * Find notification by referenceId, type, and targetResidentId
     * Used to check if notification already exists to avoid duplicate FCM push
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.referenceId = :referenceId
        AND n.type = :type
        AND n.targetResidentId = :targetResidentId
        AND n.deletedAt IS NULL
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByReferenceIdAndTypeAndTargetResidentId(
            @Param("referenceId") UUID referenceId,
            @Param("type") NotificationType type,
            @Param("targetResidentId") UUID targetResidentId
    );
    
    /**
     * Optimized query: Filter and paginate at database level for resident notifications
     * This replaces the inefficient in-memory filtering approach
     * Uses Pageable to paginate at database level (LIMIT/OFFSET)
     * 
     * Logic:
     * 1. Card notifications (CARD_FEE_REMINDER, CARD_APPROVED, CARD_REJECTED): 
     *    - Must have targetResidentId = :residentId
     * 2. Other notifications with targetResidentId: 
     *    - Must have targetResidentId = :residentId
     * 3. Notifications with targetBuildingId: 
     *    - Must have targetBuildingId = :buildingId OR targetBuildingId IS NULL (all buildings)
     * 4. Notifications without targetResidentId and without targetBuildingId: 
     *    - Show to all residents
     */
    @Query("""
        SELECT n FROM Notification n
        WHERE n.scope = :scope
        AND n.deletedAt IS NULL
        AND (
            (n.type IN ('CARD_FEE_REMINDER', 'CARD_APPROVED', 'CARD_REJECTED') 
             AND n.targetResidentId = :residentId)
            OR
            (n.type NOT IN ('CARD_FEE_REMINDER', 'CARD_APPROVED', 'CARD_REJECTED') 
             AND n.targetResidentId = :residentId)
            OR
            (n.targetResidentId IS NULL 
             AND (n.targetBuildingId IS NULL OR n.targetBuildingId = :buildingId))
        )
    """)
    Page<Notification> findNotificationsForResidentOptimized(
            @Param("scope") NotificationScope scope,
            @Param("residentId") UUID residentId,
            @Param("buildingId") UUID buildingId,
            Pageable pageable
    );
    
    /**
     * Optimized count query: Count at database level instead of loading all into memory
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.scope = :scope
        AND n.deletedAt IS NULL
        AND (
            (n.type IN ('CARD_FEE_REMINDER', 'CARD_APPROVED', 'CARD_REJECTED') 
             AND n.targetResidentId = :residentId)
            OR
            (n.type NOT IN ('CARD_FEE_REMINDER', 'CARD_APPROVED', 'CARD_REJECTED') 
             AND n.targetResidentId = :residentId)
            OR
            (n.targetResidentId IS NULL 
             AND (n.targetBuildingId IS NULL OR n.targetBuildingId = :buildingId))
        )
    """)
    long countNotificationsForResidentOptimized(
            @Param("scope") NotificationScope scope,
            @Param("residentId") UUID residentId,
            @Param("buildingId") UUID buildingId
    );
}














