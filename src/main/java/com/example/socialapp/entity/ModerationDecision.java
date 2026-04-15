package com.example.socialapp.entity;

import com.example.socialapp.enums.DecisionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Moderation Decision Entity.
 * Records moderator decisions on flagged posts.
 */
@Entity
@Table(name = "moderation_decisions", indexes = {
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_decision_type", columnList = "decision_type"),
    @Index(name = "idx_decided_at", columnList = "decided_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionType decisionType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    /**
     * Check if decision is approval.
     */
    public boolean isApproved() {
        return DecisionType.APPROVED.equals(decisionType);
    }

    /**
     * Check if decision is rejection.
     */
    public boolean isRemoved() {
        return DecisionType.REMOVED.equals(decisionType);
    }

    /**
     * Check if decision is escalation.
     */
    public boolean isEscalated() {
        return DecisionType.ESCALATED.equals(decisionType);
    }
}
