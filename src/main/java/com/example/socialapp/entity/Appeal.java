package com.example.socialapp.entity;

import com.example.socialapp.enums.AppealStatus;
import com.example.socialapp.enums.AppealType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appeal Entity.
 * Records user appeals against bans or removed posts for review and potential reversal.
 * Supports both ban appeals and post appeals through polymorphic design.
 */
@Entity
@Table(name = "appeals", indexes = {
    @Index(name = "idx_ban_id", columnList = "ban_id"),
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_appeal_type", columnList = "appeal_type"),
    @Index(name = "idx_appeal_status", columnList = "appeal_status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppealType appealType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ban_id", nullable = true)
    private Ban ban;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = true)
    private Post post;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppealStatus status;

    @Column(columnDefinition = "TEXT")
    private String moderatorReview;

    @Column(columnDefinition = "TEXT")
    private String adminDecision;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if appeal is pending.
     */
    public boolean isPending() {
        return AppealStatus.PENDING.equals(status);
    }

    /**
     * Check if appeal is approved.
     */
    public boolean isApproved() {
        return AppealStatus.APPROVED.equals(status);
    }

    /**
     * Check if appeal is rejected.
     */
    public boolean isRejected() {
        return AppealStatus.REJECTED.equals(status);
    }

    /**
     * Check if appeal requires admin review.
     * Initially flagged if moderator review is positive.
     */
    public boolean requiresAdminReview() {
        return AppealStatus.PENDING.equals(status) && moderatorReview != null && 
               moderatorReview.toLowerCase().contains("recommend");
    }
}
