package com.example.socialapp.enums;

/**
 * Enum representing the status of a user appeal against moderation decisions.
 * PENDING: Appeal is awaiting review
 * APPROVED: Appeal was approved and decision was overturned
 * REJECTED: Appeal was rejected and original decision stands
 */
public enum AppealStatus {
    PENDING("Appeal is awaiting review"),
    APPROVED("Appeal was approved and decision overturned"),
    REJECTED("Appeal was rejected");

    private final String description;

    AppealStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
