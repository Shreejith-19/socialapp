package com.example.socialapp.service;

import com.example.socialapp.dto.NotificationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

/**
 * Notification Service Interface.
 * Defines notification management operations.
 */
public interface NotificationService {

    /**
     * Notify user of a ban.
     *
     * @param userId the user ID
     * @param banType the type of ban (TEMPORARY or PERMANENT)
     * @param reason the reason for ban
     * @return the created Notification
     */
    NotificationDTO notifyUserBanned(UUID userId, String banType, String reason);

    /**
     * Notify user of appeal decision.
     *
     * @param userId the user ID
     * @param approved true if appeal was approved, false if rejected
     * @param reason the reason for decision
     * @return the created Notification
     */
    NotificationDTO notifyAppealDecision(UUID userId, boolean approved, String reason);

    /**
     * Notify user of post removal.
     *
     * @param userId the user ID (post author)
     * @param postContent the post content that was removed
     * @param reason the reason for removal
     * @return the created Notification
     */
    NotificationDTO notifyPostRemoved(UUID userId, String postContent, String reason);

    /**
     * Send custom notification to user.
     *
     * @param userId the user ID
     * @param message the notification message
     * @return the created Notification
     */
    NotificationDTO sendNotification(UUID userId, String message);

    /**
     * Get notification by ID.
     */
    NotificationDTO getNotificationById(UUID notificationId);

    /**
     * Get all notifications for a user.
     */
    List<NotificationDTO> getUserNotifications(UUID userId);

    /**
     * Get user notifications with pagination.
     */
    Page<NotificationDTO> getUserNotifications(UUID userId, Pageable pageable);

    /**
     * Get unread notifications for a user.
     */
    List<NotificationDTO> getUnreadNotifications(UUID userId);

    /**
     * Get unread notifications with pagination.
     */
    Page<NotificationDTO> getUnreadNotifications(UUID userId, Pageable pageable);

    /**
     * Count unread notifications for a user.
     */
    long getUnreadNotificationCount(UUID userId);

    /**
     * Mark notification as read.
     */
    NotificationDTO markAsRead(UUID notificationId);

    /**
     * Mark all notifications as read for a user.
     */
    void markAllAsRead(UUID userId);

    /**
     * Delete a notification.
     */
    void deleteNotification(UUID notificationId);

    /**
     * Delete all notifications for a user.
     */
    void deleteAllNotifications(UUID userId);
}
