package com.example.socialapp.service.impl;

import com.example.socialapp.dto.NotificationDTO;
import com.example.socialapp.entity.Notification;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.NotificationType;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.NotificationRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notification Service Implementation.
 * Provides notification management logic for user alerts on moderation actions.
 */
@Slf4j
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public NotificationDTO notifyUserBanned(UUID userId, String banType, String reason) {
        log.info("Notifying user {} of ban: {}", userId, banType);

        String message = String.format("You have been %s banned. Reason: %s", 
            banType.toLowerCase(), reason);

        return sendNotification(userId, message);
    }

    @Override
    public NotificationDTO notifyAppealDecision(UUID userId, boolean approved, String reason) {
        log.info("Notifying user {} of appeal decision: approved={}", userId, approved);

        String message = String.format("Your ban appeal has been %s. %s",
            approved ? "APPROVED" : "REJECTED",
            approved ? "Your ban has been lifted and you can post again." : reason);

        return sendNotification(userId, message);
    }

    @Override
    public NotificationDTO notifyPostRemoved(UUID userId, String postContent, String reason) {
        log.info("Notifying user {} of post removal", userId);

        String content = postContent.length() > 50 ? 
            postContent.substring(0, 50) + "..." : postContent;

        String message = String.format("Your post \"%s\" has been removed from the platform. Reason: %s",
            content, reason);

        return sendNotification(userId, message);
    }

    @Override
    public NotificationDTO sendNotification(UUID userId, String message) {
        log.info("Sending notification to user: {}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        Notification notification = Notification.builder()
            .user(user)
            .message(message)
            .isRead(false)
            .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification sent: {} to user: {}", savedNotification.getId(), userId);

        return mapToDTO(savedNotification);
    }

    @Override
    public NotificationDTO createNotification(User user, String message, NotificationType type) {
        log.info("Creating notification for user: {}, type: {}", user.getId(), type);

        Notification notification = Notification.builder()
            .user(user)
            .message(message)
            .type(type)
            .isRead(false)
            .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created: {} for user: {}", savedNotification.getId(), user.getId());

        return mapToDTO(savedNotification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDTO getNotificationById(UUID notificationId) {
        log.debug("Fetching notification: {}", notificationId);
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId.toString()));
        return mapToDTO(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(UUID userId) {
        log.debug("Fetching all notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(UUID userId, Pageable pageable) {
        log.debug("Fetching notifications for user {} with pagination", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        log.debug("Fetching unread notifications for user: {}", userId);
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUnreadNotifications(UUID userId, Pageable pageable) {
        log.debug("Fetching unread notifications for user {} with pagination", userId);
        return notificationRepository.findByUserIdAndIsReadFalse(userId, pageable)
            .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        log.debug("Unread notification count for user {}: {}", userId, count);
        return count;
    }

    @Override
    public NotificationDTO markAsRead(UUID notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId.toString()));

        notification.markAsRead();
        Notification updatedNotification = notificationRepository.save(notification);
        log.info("Notification marked as read: {}", notificationId);

        return mapToDTO(updatedNotification);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for user: {}", userId);

        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unreadNotifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);

        log.info("Marked {} notifications as read for user: {}", unreadNotifications.size(), userId);
    }

    @Override
    public void deleteNotification(UUID notificationId) {
        log.info("Deleting notification: {}", notificationId);

        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId.toString());
        }

        notificationRepository.deleteById(notificationId);
        log.info("Notification deleted: {}", notificationId);
    }

    @Override
    public void deleteAllNotifications(UUID userId) {
        log.info("Deleting all notifications for user: {}", userId);

        List<Notification> notifications = notificationRepository.findByUserId(userId);
        notificationRepository.deleteAll(notifications);

        log.info("Deleted {} notifications for user: {}", notifications.size(), userId);
    }

    /**
     * Map Notification entity to NotificationDTO.
     */
    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
            .id(notification.getId())
            .userId(notification.getUser().getId())
            .message(notification.getMessage())
            .type(notification.getType())
            .isRead(notification.getIsRead())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
