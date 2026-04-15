package com.example.socialapp.dto;

import com.example.socialapp.enums.AppealStatus;
import com.example.socialapp.enums.AppealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Appeal DTO.
 * Data transfer object for appeals (both ban and post appeals).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppealDTO {
    private UUID id;
    private AppealType appealType;
    private UUID banId;
    private UUID postId;
    private String username;  // User who submitted the appeal
    private String reason;
    private AppealStatus status;
    private String moderatorReview;
    private String adminDecision;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
