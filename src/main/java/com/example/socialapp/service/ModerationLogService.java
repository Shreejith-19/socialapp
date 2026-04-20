package com.example.socialapp.service;

import com.example.socialapp.entity.User;
import com.example.socialapp.enums.DecisionType;
import java.util.UUID;

/**
 * Moderation Log Service Interface.
 * Defines moderation logging operations.
 */
public interface ModerationLogService {

    /**
     * Log a moderation action.
     *
     * @param moderator the moderator user
     * @param postId the ID of the post being actioned
     * @param action the decision type (APPROVED or REMOVED)
     */
    void logAction(User moderator, UUID postId, DecisionType action);
}
