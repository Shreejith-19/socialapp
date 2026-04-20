package com.example.socialapp.controller;

import com.example.socialapp.dto.NotificationDTO;
import com.example.socialapp.service.NotificationService;
import com.example.socialapp.util.SecurityUtil;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * Notification Controller.
 * Handles notification management endpoints for user alerts.
 */
@Slf4j
@RestController
@RequestMapping("/v1/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Get all notifications for current user.
     * Requires USER role.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications() {
        log.info("Fetching all notifications for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        List<NotificationDTO> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get user notifications with pagination.
     * Requires USER role.
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<NotificationDTO>> getUserNotificationsPaginated(
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching notifications with pagination for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        Page<NotificationDTO> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for current user.
     * Requires USER role.
     */
    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        log.info("Fetching unread notifications for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications with pagination.
     * Requires USER role.
     */
    @GetMapping("/unread/page")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<NotificationDTO>> getUnreadNotificationsPaginated(
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching unread notifications with pagination for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        Page<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get count of unread notifications.
     * Requires USER role.
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        log.info("Fetching unread notification count for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        long count = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(UnreadCountResponse.builder().unreadCount(count).build());
    }

    /**
     * Get notification by ID.
     * Requires USER role.
     */
    @GetMapping("/{notificationId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NotificationDTO> getNotificationById(@PathVariable UUID notificationId) {
        log.info("Fetching notification: {}", notificationId);
        NotificationDTO notification = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark notification as read.
     * Requires USER role.
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable UUID notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        NotificationDTO notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark all notifications as read for current user.
     * Requires USER role.
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> markAllAsRead() {
        log.info("Marking all notifications as read for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a notification.
     * Requires USER role.
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID notificationId) {
        log.info("Deleting notification: {}", notificationId);
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all notifications for current user.
     * Requires USER role.
     */
    @DeleteMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteAllNotifications() {
        log.info("Deleting all notifications for current user");

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID userId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Response DTO for unread count.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class UnreadCountResponse {
        private long unreadCount;
    }
}
