package com.example.socialapp.service;

import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.ModerationDecision;
import com.example.socialapp.enums.DecisionType;

/**
 * Moderation Service Interface.
 * Defines content moderation operations.
 */
public interface ModerationService {
    
    /**
     * Review content using NLP model simulation.
     * Checks content against banned keywords and patterns.
     *
     * @param content the content to review
     * @return true if content is clean (safe), false if content is inappropriate
     */
    boolean reviewContent(String content);
    
    /**
     * Flag a post for moderation review.
     * Changes post status to FLAGGED and stores for moderator review.
     *
     * @param post the post to flag
     */
    void flagPost(Post post);
    
    /**
     * Send a post to the moderation queue for processing.
     * Queues the post for async moderation workflow.
     *
     * @param post the post to add to moderation queue
     */
    void sendToQueue(Post post);
    
    /**
     * Get the count of posts pending moderation.
     *
     * @return the number of posts in the moderation queue
     */
    int getPendingModerationCount();
    
    /**
     * Approve a post (remove from queue and keep PUBLISHED status).
     *
     * @param post the post to approve
     */
    void approvePost(Post post);
    
    /**
     * Reject a post (remove from queue and set status to REMOVED).
     *
     * @param post the post to reject
     */
    void rejectPost(Post post);

    /**
     * Process a moderation decision.
     * Applies the decision based on decision type:
     * - APPROVED: Set post to PUBLISHED and remove from queue
     * - REMOVED: Set post to REMOVED and apply penalty to author
     * - ESCALATED: Keep in queue for senior review
     *
     * @param decisionType the decision type
     * @param postId the post ID
     * @param reason the reason for decision
     * @return the created ModerationDecision
     */
    ModerationDecision makeDecision(DecisionType decisionType, java.util.UUID postId, String reason);

    /**
     * Get a moderation decision by post ID.
     *
     * @param postId the post ID
     * @return the ModerationDecision if exists
     */
    java.util.Optional<ModerationDecision> getDecisionByPostId(java.util.UUID postId);
}
