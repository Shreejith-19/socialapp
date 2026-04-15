package com.example.socialapp.service;

import com.example.socialapp.dto.AppealDTO;
import com.example.socialapp.enums.AppealStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Appeal Service Interface.
 * Defines appeal management operations for ban and post appeal reviews.
 */
public interface AppealService {

    /**
     * Submit an appeal against a ban.
     * User provides reason for appeal.
     *
     * @param banId the ban ID to appeal
     * @param reason the reason for appeal
     * @return the created Appeal
     */
    AppealDTO submitAppeal(UUID banId, String reason);

    /**
     * Submit an appeal for a removed/rejected post.
     * User can only appeal posts they authored.
     *
     * @param postId the post ID to appeal
     * @param reason the reason for appeal
     * @return the created Appeal
     * @throws IllegalStateException if post is not in REMOVED status
     * @throws ConflictException if user is not the post author or appeal already exists
     */
    AppealDTO submitPostAppeal(UUID postId, String reason);

    /**
     * Get appeal by ID.
     */
    AppealDTO getAppealById(UUID appealId);

    /**
     * Get appeal by ban ID.
     */
    Optional<AppealDTO> getAppealByBanId(UUID banId);

    /**
     * Get appeal by post ID.
     */
    Optional<AppealDTO> getAppealByPostId(UUID postId);

    /**
     * Get all appeals by status.
     */
    List<AppealDTO> getAppealsByStatus(AppealStatus status);

    /**
     * Get pending appeals for moderator review (ordered by creation date).
     */
    List<AppealDTO> getPendingAppeals();

    /**
     * Moderator review of appeal.
     * Moderator provides review comments.
     *
     * @param appealId the appeal ID
     * @param review the moderator review
     * @return updated Appeal
     */
    AppealDTO moderatorReview(UUID appealId, String review);

    /**
     * Admin final decision on appeal.
     * Admin approves or rejects based on moderator review.
     *
     * @param appealId the appeal ID
     * @param approved true to approve, false to reject
     * @param decision the admin decision reason
     * @return updated Appeal
     */
    AppealDTO adminDecision(UUID appealId, boolean approved, String decision);

    /**
     * Get count of pending appeals.
     */
    long getPendingAppealCount();

    /**
     * Get count of appeals by status.
     */
    long getAppealCountByStatus(AppealStatus status);
}
