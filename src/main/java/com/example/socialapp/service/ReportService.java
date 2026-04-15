package com.example.socialapp.service;

import com.example.socialapp.dto.ReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

/**
 * Report Service Interface.
 * Defines report management operations for post moderation.
 */
public interface ReportService {

    /**
     * Submit a report against a post.
     * User reports a post with a reason.
     * Prevents duplicate reports from same user for same post.
     *
     * @param postId the post ID to report
     * @param reporterId the reporter user ID
     * @param reason the reason for report
     * @return the created Report
     */
    ReportDTO submitReport(UUID postId, UUID reporterId, String reason);

    /**
     * Get report by ID.
     */
    ReportDTO getReportById(UUID reportId);

    /**
     * Get all reports for a post.
     */
    List<ReportDTO> getReportsByPostId(UUID postId);

    /**
     * Get reports for a post with pagination.
     */
    Page<ReportDTO> getReportsByPostId(UUID postId, Pageable pageable);

    /**
     * Get all reports by a reporter.
     */
    List<ReportDTO> getReportsByReporterId(UUID reporterId);

    /**
     * Get all reports ordered by creation date (for moderator review queue).
     */
    List<ReportDTO> getAllReports();

    /**
     * Get all reports with pagination.
     */
    Page<ReportDTO> getAllReports(Pageable pageable);

    /**
     * Get count of reports for a post.
     * Helps determine if post needs immediate review.
     */
    long getReportCountForPost(UUID postId);

    /**
     * Check if user has already reported a post.
     */
    boolean hasUserReportedPost(UUID postId, UUID reporterId);

    /**
     * Delete a report (when action is taken on post).
     */
    void deleteReport(UUID reportId);

    /**
     * Get count of all reports (for dashboard).
     */
    long getTotalReportCount();
}
