package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Like information.
 * Matches backend LikeDTO structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeDTO {
    private UUID id;
    private UUID userId;
    private String userUsername;
    private UUID postId;
    private Boolean isLike;
    private LocalDateTime createdAt;
}
