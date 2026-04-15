package com.example.socialapp.enums;

/**
 * Enum representing the type of penalty applied to a user.
 * WARNING: User receives a warning (informational)
 * TEMP_BAN: User is temporarily banned from the platform
 * PERM_BAN: User is permanently banned from the platform
 */
public enum PenaltyType {
    WARNING("User receives a warning"),
    TEMP_BAN("User is temporarily banned"),
    PERM_BAN("User is permanently banned");

    private final String description;

    PenaltyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
