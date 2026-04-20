package com.example.socialapp.enums;

/**
 * Notification Types enum.
 * Defines different types of notifications users can receive.
 */
public enum NotificationType {
    POST_PUBLISHED("Post Published", "Your post has been published"),
    POST_FLAGGED("Post Flagged", "Your post has been flagged for review"),
    POST_APPROVED("Post Approved", "Your flagged post has been approved"),
    POST_REMOVED("Post Removed", "Your post has been removed"),
    TEMP_BAN("Temporary Ban", "You have been temporarily banned"),
    PERM_BAN("Permanent Ban", "You have been permanently banned"),
    APPEAL_APPROVED("Appeal Approved", "Your appeal has been approved"),
    APPEAL_REJECTED("Appeal Rejected", "Your appeal has been rejected"),
    COMMENT_RECEIVED("New Comment", "Someone commented on your post");

    private final String title;
    private final String description;

    NotificationType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
