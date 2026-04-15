package com.example.socialapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Like information.
 * Used for transferring like data between layers.
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
