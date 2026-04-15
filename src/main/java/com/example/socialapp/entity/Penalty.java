package com.example.socialapp.entity;

import com.example.socialapp.enums.PenaltyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Penalty Entity.
 * Records penalties issued to users for rule violations.
 */
@Entity
@Table(name = "penalties", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_penalty_type", columnList = "penalty_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PenaltyType type;

    @Column(nullable = false)
    private Integer points;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if penalty is warning.
     */
    public boolean isWarning() {
        return PenaltyType.WARNING.equals(type);
    }

    /**
     * Check if penalty is temporary ban.
     */
    public boolean isTemporaryBan() {
        return PenaltyType.TEMP_BAN.equals(type);
    }

    /**
     * Check if penalty is permanent ban.
     */
    public boolean isPermanentBan() {
        return PenaltyType.PERM_BAN.equals(type);
    }
}
