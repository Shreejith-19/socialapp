package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Moderation Decision information.
 * Records moderator decisions on flagged posts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationDecisionDTO {
    private UUID id;
    private UUID postId;
    private String decisionType;
    private String reason;
    private LocalDateTime decidedAt;
}
