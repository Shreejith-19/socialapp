package com.example.socialapp.enums;

/**
 * Enum representing the status of a user in the system.
 * ACTIVE: User account is active and can perform operations
 * TEMP_BANNED: User is temporarily banned and cannot perform certain operations
 * PERM_BANNED: User is permanently banned from the system
 */
public enum UserStatus {
    ACTIVE("User account is active"),
    TEMP_BANNED("User is temporarily banned"),
    PERM_BANNED("User is permanently banned");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
