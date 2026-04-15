package com.example.socialapp.repository;

import com.example.socialapp.entity.Report;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Report Repository.
 * Data access for post reports.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Find all reports for a post.
     */
    List<Report> findByPost(Post post);

    /**
     * Find all reports for a post with pagination.
     */
    Page<Report> findByPost(Post post, Pageable pageable);

    /**
     * Find reports for a post ID.
     */
    List<Report> findByPostId(UUID postId);

    /**
     * Find reports for a post ID with pagination.
     */
    Page<Report> findByPostId(UUID postId, Pageable pageable);

    /**
     * Find all reports by a reporter user.
     */
    List<Report> findByReporter(User reporter);

    /**
     * Find reports by reporter ID.
     */
    List<Report> findByReporterId(UUID reporterId);

    /**
     * Find all reports ordered by creation date (newest first).
     */
    List<Report> findAllByOrderByCreatedAtDesc();

    /**
     * Find reports ordered by creation date with pagination.
     */
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count total reports for a post.
     */
    long countByPost(Post post);

    /**
     * Count reports by post ID.
     */
    long countByPostId(UUID postId);

    /**
     * Count reports by reporter.
     */
    long countByReporter(User reporter);

    /**
     * Check if user has already reported a post (prevent duplicate reports).
     */
    boolean existsByPostAndReporter(Post post, User reporter);

    /**
     * Check if user has reported a specific post ID.
     */
    boolean existsByPostIdAndReporterId(UUID postId, UUID reporterId);
}
