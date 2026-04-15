package com.example.socialapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Report DTO.
 * Data transfer object for reports.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {
    private UUID id;
    private UUID postId;
    private String postContent;
    private UUID reporterId;
    private String reporterUsername;
    private String reason;
    private LocalDateTime createdAt;

    /**
     * Get summary of report reason.
     */
    public String getReasonSummary() {
        return reason.substring(0, Math.min(100, reason.length()));
    }
}
