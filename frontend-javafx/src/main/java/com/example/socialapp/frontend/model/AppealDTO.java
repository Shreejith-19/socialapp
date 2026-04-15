package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Appeal information.
 * Matches backend AppealDTO structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealDTO {
    private UUID id;
    private String appealType;  // BAN or POST
    private UUID banId;
    private UUID postId;
    private String username;  // User who submitted the appeal
    private String reason;
    private String status;  // PENDING, APPROVED, REJECTED
    private String moderatorReview;
    private String adminDecision;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
