package com.example.socialapp.controller;

import com.example.socialapp.dto.ReportDTO;
import com.example.socialapp.service.ReportService;
import com.example.socialapp.util.SecurityUtil;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Report Controller.
 * Handles post report submission and moderator review endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/v1/reports")
@Validated
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    /**
     * Submit a report against a post.
     * Requires USER role.
     * Users can report posts they find inappropriate.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<ReportDTO> submitReport(@Valid @RequestBody ReportRequest request) {
        log.info("Report submission request for post: {}", request.getPostId());

        // Get current user ID
        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID reporterId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        ReportDTO report = reportService.submitReport(request.getPostId(), reporterId, request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Get report by ID.
     * Requires MODERATOR role.
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable UUID reportId) {
        log.info("Fetching report: {}", reportId);
        ReportDTO report = reportService.getReportById(reportId);
        return ResponseEntity.ok(report);
    }

    /**
     * Get all reports for a specific post.
     * Requires MODERATOR role.
     */
    @GetMapping("/post/{postId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<List<ReportDTO>> getReportsByPostId(@PathVariable UUID postId) {
        log.info("Fetching reports for post: {}", postId);
        List<ReportDTO> reports = reportService.getReportsByPostId(postId);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get all reports for a post with pagination.
     * Requires MODERATOR role.
     */
    @GetMapping("/post/{postId}/page")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<ReportDTO>> getReportsByPostIdPaginated(
            @PathVariable UUID postId,
            Pageable pageable) {
        log.info("Fetching reports for post {} with pagination", postId);
        Page<ReportDTO> reports = reportService.getReportsByPostId(postId, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get all reports (moderator review queue).
     * Requires MODERATOR role.
     * Returns reports ordered by newest first.
     */
    @GetMapping
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        log.info("Fetching all reports for moderator review");
        List<ReportDTO> reports = reportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Get all reports with pagination.
     * Requires MODERATOR role.
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Page<ReportDTO>> getAllReportsPaginated(Pageable pageable) {
        log.info("Fetching all reports with pagination for moderator review");
        Page<ReportDTO> reports = reportService.getAllReports(pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Get report count for a post.
     * Helps determine if post needs immediate review.
     * Requires MODERATOR role.
     */
    @GetMapping("/post/{postId}/count")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<ReportCountResponse> getReportCount(@PathVariable UUID postId) {
        log.info("Fetching report count for post: {}", postId);
        long count = reportService.getReportCountForPost(postId);
        return ResponseEntity.ok(ReportCountResponse.builder().count(count).build());
    }

    /**
     * Get total reports count (for dashboard).
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('MODERATOR') || hasRole('ADMIN')")
    public ResponseEntity<TotalReportCountResponse> getTotalReportCount() {
        log.info("Fetching total report count");
        long count = reportService.getTotalReportCount();
        return ResponseEntity.ok(TotalReportCountResponse.builder().totalCount(count).build());
    }

    /**
     * Check if user has already reported a post.
     * For frontend UI to prevent duplicate reports.
     */
    @GetMapping("/check/{postId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReportCheckResponse> checkIfReported(@PathVariable UUID postId) {
        log.debug("Checking if user has reported post: {}", postId);

        String userEmail = SecurityUtil.getCurrentUserEmail();
        UUID reporterId = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail))
            .getId();

        boolean hasReported = reportService.hasUserReportedPost(postId, reporterId);
        return ResponseEntity.ok(ReportCheckResponse.builder().hasReported(hasReported).build());
    }

    /**
     * Delete a report (when action is taken on post).
     * Requires MODERATOR role.
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID reportId) {
        log.info("Deleting report: {}", reportId);
        reportService.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Request DTO for report submission.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReportRequest {
        @NotNull(message = "Post ID is required")
        private UUID postId;

        @NotBlank(message = "Reason is required")
        private String reason;
    }

    /**
     * Response DTO for report count.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ReportCountResponse {
        private long count;
    }

    /**
     * Response DTO for total report count.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TotalReportCountResponse {
        private long totalCount;
    }

    /**
     * Response DTO for report check.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ReportCheckResponse {
        private boolean hasReported;
    }
}
