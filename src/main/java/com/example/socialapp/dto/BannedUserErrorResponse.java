package com.example.socialapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response for banned users.
 * Includes ban information such as remaining time for temporary bans.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannedUserErrorResponse {
    private int status;
    private String message;
    private String error;
    private String path;
    
    // Ban-specific fields
    private boolean isBanned;
    private String banType;  // TEMPORARY or PERMANENT
    private Long remainingDays;
    private Long remainingHours;
    private String banMessage;  // Formatted message like "You are temporarily banned. X days remaining"
}
