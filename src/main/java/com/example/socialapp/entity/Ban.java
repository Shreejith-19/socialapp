package com.example.socialapp.entity;

import com.example.socialapp.enums.BanType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Ban Entity.
 * Records user bans with temporal constraints.
 */
@Entity
@Table(name = "bans", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_ban_type", columnList = "ban_type"),
    @Index(name = "idx_end_date", columnList = "end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ban {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BanType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startDate;

    @Column(nullable = true)
    private LocalDateTime endDate;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if ban is active.
     * Returns true if:
     * - Ban type is PERMANENT, OR
     * - Ban type is TEMPORARY and end date is in the future
     */
    public boolean isActive() {
        if (BanType.PERMANENT.equals(type)) {
            return true;
        }
        if (BanType.TEMPORARY.equals(type) && endDate != null) {
            return LocalDateTime.now().isBefore(endDate);
        }
        return false;
    }

    /**
     * Check if ban is expired.
     * Only applies to temporary bans.
     */
    public boolean isExpired() {
        if (BanType.TEMPORARY.equals(type) && endDate != null) {
            return LocalDateTime.now().isAfter(endDate);
        }
        return false;
    }

    /**
     * Check if ban is permanent.
     */
    public boolean isPermanent() {
        return BanType.PERMANENT.equals(type);
    }

    /**
     * Check if ban is temporary.
     */
    public boolean isTemporary() {
        return BanType.TEMPORARY.equals(type);
    }

    /**
     * Get remaining days for temporary ban.
     * Returns 0 if ban is expired or not temporary.
     */
    public long getRemainingDays() {
        if (!isTemporary() || endDate == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endDate)) {
            return 0; // Ban expired
        }
        return ChronoUnit.DAYS.between(now, endDate);
    }

    /**
     * Get remaining hours for temporary ban.
     * Returns 0 if ban is expired or not temporary.
     * Useful when less than 1 day remains.
     */
    public long getRemainingHours() {
        if (!isTemporary() || endDate == null) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endDate)) {
            return 0; // Ban expired
        }
        return ChronoUnit.HOURS.between(now, endDate);
    }

    /**
     * Get formatted remaining ban message.
     * Returns message like "X days remaining" or "X hours remaining"
     * Returns empty string if ban is not active or not temporary.
     */
    public String getFormattedRemainingTime() {
        if (!isActive() || !isTemporary()) {
            return "";
        }
        long days = getRemainingDays();
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " remaining";
        }
        long hours = getRemainingHours();
        if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " remaining";
        }
        return "Less than 1 hour remaining";
    }
}
