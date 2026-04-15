package com.example.socialapp.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for post appeal submission.
 * Contains the post ID and appeal reason.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostAppealRequest {
    
    @NotNull(message = "Post ID is required")
    private UUID postId;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}
