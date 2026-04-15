package com.example.socialapp.frontend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Post information.
 * Matches backend PostDTO structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    private UUID id;
    private String content;
    private String status;
    private UUID authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Get post summary (first 100 characters).
     */
    public String getSummary() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
