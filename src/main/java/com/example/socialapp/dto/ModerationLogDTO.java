package com.example.socialapp.dto;

import com.example.socialapp.enums.ModerationLogAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Moderation Log Data Transfer Object.
 * Used to return moderation log information to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationLogDTO {

    private UUID id;

    private UUID moderatorId;

    private String moderatorName;

    private ModerationLogAction action;

    private UUID postId;

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
