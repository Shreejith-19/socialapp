package com.example.socialapp.dto;

import com.example.socialapp.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification DTO.
 * Data transfer object for notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private UUID id;
    private UUID userId;
    private String message;
    private NotificationType type;
    private Boolean isRead;
    private LocalDateTime createdAt;

    /**
     * Get summary of notification (first 100 chars).
     */
    public String getSummary() {
        return message.substring(0, Math.min(100, message.length()));
    }
}
