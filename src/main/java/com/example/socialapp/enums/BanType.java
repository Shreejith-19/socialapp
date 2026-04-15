package com.example.socialapp.enums;

/**
 * Enum representing the type of ban applied to a user.
 * TEMPORARY: User is banned for a limited period
 * PERMANENT: User is banned indefinitely
 */
public enum BanType {
    TEMPORARY("Ban is temporary with an end date"),
    PERMANENT("Ban is permanent with no end date");

    private final String description;

    BanType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
