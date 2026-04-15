package com.example.socialapp.enums;

/**
 * Enum representing the decision type made by moderators on flagged content.
 * APPROVED: Content was reviewed and approved (no action taken)
 * REMOVED: Content was removed due to policy violation
 * ESCALATED: Content requires further review by senior moderators
 */
public enum DecisionType {
    APPROVED("Content approved, no action required"),
    REMOVED("Content removed due to policy violation"),
    ESCALATED("Content escalated for further review");

    private final String description;

    DecisionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
