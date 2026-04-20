package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * DTO for Moderation Log information.
 * Matches backend ModerationLogDTO structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationLogDTO {
    private UUID id;
    private UUID moderatorId;
    private String moderatorName;
    private String action;  // APPROVED or REMOVED
    private UUID postId;
    private LocalDateTime createdAt;

    /**
     * Get formatted timestamp for display.
     */
    public String getFormattedTimestamp() {
        if (createdAt == null) {
            return "N/A";
        }
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Check if action is approval.
     */
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(action);
    }

    /**
     * Check if action is removal.
     */
    public boolean isRemoved() {
        return "REMOVED".equalsIgnoreCase(action);
    }
}
