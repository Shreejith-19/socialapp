package com.example.socialapp.repository;

import com.example.socialapp.entity.Notification;
import com.example.socialapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Notification Repository.
 * Data access for user notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Find all notifications for a user.
     */
    List<Notification> findByUser(User user);

    /**
     * Find notifications for a user with pagination.
     */
    Page<Notification> findByUser(User user, Pageable pageable);

    /**
     * Find notifications for a user ID.
     */
    List<Notification> findByUserId(UUID userId);

    /**
     * Find notifications for a user ID with pagination.
     */
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find unread notifications for a user.
     */
    List<Notification> findByUserAndIsReadFalse(User user);

    /**
     * Find unread notifications for a user ID.
     */
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);

    /**
     * Find unread notifications sorted by creation date (newest first).
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * Find unread notifications with pagination.
     */
    Page<Notification> findByUserIdAndIsReadFalse(UUID userId, Pageable pageable);

    /**
     * Count unread notifications for a user.
     */
    long countByUserAndIsReadFalse(User user);

    /**
     * Count unread notifications for a user ID.
     */
    long countByUserIdAndIsReadFalse(UUID userId);

    /**
     * Find notifications ordered by creation date (newest first).
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find notifications ordered by creation date with pagination.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find notifications for a user ordered by creation date (newest first).
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
}
