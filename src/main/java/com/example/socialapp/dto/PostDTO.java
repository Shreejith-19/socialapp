package com.example.socialapp.dto;

import com.example.socialapp.enums.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Post information.
 * Used for transferring post data between layers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    private UUID id;
    private String content;
    private PostStatus status;
    private UUID authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Get post summary (first 100 characters).
     *
     * @return post summary
     */
    public String getSummary() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
}
