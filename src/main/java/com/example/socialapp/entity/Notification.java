package com.example.socialapp.entity;

import com.example.socialapp.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Entity.
 * Records notifications sent to users about moderation actions.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_is_read", columnList = "is_read"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Mark notification as read.
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * Mark notification as unread.
     */
    public void markAsUnread() {
        this.isRead = false;
    }

    /**
     * Check if notification is unread.
     */
    public boolean isUnread() {
        return !isRead;
    }

    /**
     * Get summary of notification (first 100 chars).
     */
    public String getSummary() {
        return message.substring(0, Math.min(100, message.length()));
    }
}
