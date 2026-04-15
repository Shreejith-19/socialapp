package com.example.socialapp.enums;

/**
 * Enum representing the status of a post in the system.
 * PUBLISHED: Post is visible to other users
 * FLAGGED: Post has been flagged for review by moderators
 * REMOVED: Post has been removed from the platform
 */
public enum PostStatus {
    PUBLISHED("Post is visible to users"),
    FLAGGED("Post is flagged for review"),
    REMOVED("Post has been removed");

    private final String description;

    PostStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
