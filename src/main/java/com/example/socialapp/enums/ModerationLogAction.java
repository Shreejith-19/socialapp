package com.example.socialapp.enums;

/**
 * Enum representing moderation log actions.
 * APPROVED: Post was approved and kept visible
 * REMOVED: Post was removed due to policy violation
 */
public enum ModerationLogAction {
    APPROVED("Post approved by moderator"),
    REMOVED("Post removed by moderator");

    private final String description;

    ModerationLogAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
