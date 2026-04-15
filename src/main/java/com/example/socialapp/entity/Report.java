package com.example.socialapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Report Entity.
 * Records user reports against posts for moderation review.
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_reporter_id", columnList = "reporter_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Get a summary of the report.
     */
    public String getSummary() {
        return reason.substring(0, Math.min(100, reason.length()));
    }
}
