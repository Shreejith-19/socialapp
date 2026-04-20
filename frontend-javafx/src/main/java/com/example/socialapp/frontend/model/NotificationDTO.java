package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Data Transfer Object.
 * Represents a user notification in the frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    
    private UUID id;
    
    private UUID userId;
    
    private String message;
    
    private String type;  // POST_PUBLISHED, POST_FLAGGED, POST_APPROVED, TEMP_BAN, APPEAL_APPROVED, etc.
    
    private boolean isRead;
    
    private LocalDateTime createdAt;
    
    /**
     * Get formatted creation time.
     */
    public String getFormattedTime() {
        if (createdAt == null) return "Unknown";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.temporal.ChronoUnit.MINUTES.between(createdAt, now);
        long hours = java.time.temporal.ChronoUnit.HOURS.between(createdAt, now);
        long days = java.time.temporal.ChronoUnit.DAYS.between(createdAt, now);
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else {
            return createdAt.toLocalDate().toString();
        }
    }
    
    /**
     * Get notification type badge class.
     */
    public String getTypeBadgeClass() {
        if (type == null) return "badge-default";
        return switch (type.toUpperCase()) {
            case "POST_PUBLISHED" -> "badge-success";
            case "POST_FLAGGED" -> "badge-warning";
            case "POST_APPROVED" -> "badge-info";
            case "TEMP_BAN" -> "badge-danger";
            case "APPEAL_APPROVED" -> "badge-success";
            case "APPEAL_REJECTED" -> "badge-danger";
            default -> "badge-default";
        };
    }
}
