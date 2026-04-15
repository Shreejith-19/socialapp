package com.example.socialapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for User information.
 * Used for transferring user data between layers.
 * Includes ban information for temporary ban handling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Ban information
    private String status;  // USER, TEMP_BANNED, PERM_BANNED
    private Boolean isBanned;  // true if user is currently banned
    private Long remainingBanDays;  // Days remaining for temporary ban
    private Long remainingBanHours;  // Hours remaining for temporary ban (if < 1 day)
    private String banMessage;  // Formatted message like "You are temporarily banned. X days remaining"

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
