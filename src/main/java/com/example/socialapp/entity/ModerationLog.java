package com.example.socialapp.entity;

import com.example.socialapp.enums.ModerationLogAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Moderation Log Entity.
 * Records all moderation actions taken by moderators on posts.
 */
@Entity
@Table(name = "moderation_logs", indexes = {
    @Index(name = "idx_moderator_id", columnList = "moderator_id"),
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "moderator_id", nullable = false)
    private UUID moderatorId;

    @Column(name = "moderator_name", nullable = false, length = 255)
    private String moderatorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationLogAction action;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if action is approval.
     */
    public boolean isApproved() {
        return ModerationLogAction.APPROVED.equals(action);
    }

    /**
     * Check if action is removal.
     */
    public boolean isRemoved() {
        return ModerationLogAction.REMOVED.equals(action);
    }
}
