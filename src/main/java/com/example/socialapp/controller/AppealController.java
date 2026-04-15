package com.example.socialapp.controller;

import com.example.socialapp.dto.AppealDTO;
import com.example.socialapp.enums.AppealStatus;
import com.example.socialapp.service.AppealService;
import lombok.extern.slf4j.Slf4j;
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
 * Appeal Controller.
 * Handles appeal submission and review endpoints for ban and post appeals with role-based access control.
 * 
 * USER Role Endpoints:
 * - POST /api/v1/appeals - Submit an appeal against a ban
 * - POST /api/v1/appeals/posts - Submit an appeal for a removed post
 * 
 * MODERATOR Role Endpoints (MODERATOR or ADMIN):
 * - GET /api/v1/appeals/{appealId} - Get appeal by ID
 * - GET /api/v1/appeals/pending - Get all pending appeals for moderator review
 * - GET /api/v1/appeals/status/{status} - Get appeals by status
 * - GET /api/v1/appeals/pending/count - Get pending appeals count
 * - PUT /api/v1/appeals/{appealId}/review - Moderator review of appeal
 * - PUT /api/v1/appeals/{appealId}/decision - Moderator/Admin decision to approve or reject appeal
 */
@Slf4j
@RestController
@RequestMapping("/v1/appeals")
@Validated
public class AppealController {

    private final AppealService appealService;

    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    /**
     * Submit an appeal against a ban.
     * Requires USER role.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<AppealDTO> submitAppeal(@Valid @RequestBody AppealRequest request) {
        log.info("Appeal submission request for ban: {}", request.getBanId());
        AppealDTO appeal = appealService.submitAppeal(request.getBanId(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(appeal);
    }

    /**
     * Submit an appeal for a removed/rejected post.
     * User can only appeal posts they authored.
     * Requires USER role.
     */
    @PostMapping("/posts")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<AppealDTO> submitPostAppeal(@Valid @RequestBody PostAppealRequest request) {
        log.info("Post appeal submission request for post: {}", request.getPostId());
        AppealDTO appeal = appealService.submitPostAppeal(request.getPostId(), request.getReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(appeal);
    }

    /**
     * Get appeal by ID.
     * Accessible by moderators and admins.
     */
    @GetMapping("/{appealId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<AppealDTO> getAppealById(@PathVariable UUID appealId) {
        log.info("Fetching appeal: {}", appealId);
        AppealDTO appeal = appealService.getAppealById(appealId);
        return ResponseEntity.ok(appeal);
    }

    /**
     * Get all pending appeals for moderator review.
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<AppealDTO>> getPendingAppeals() {
        log.info("Fetching pending appeals for moderator review");
        List<AppealDTO> appeals = appealService.getPendingAppeals();
        return ResponseEntity.ok(appeals);
    }

    /**
     * Get all appeals by status.
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<AppealDTO>> getAppealsByStatus(@PathVariable AppealStatus status) {
        log.info("Fetching appeals with status: {}", status);
        List<AppealDTO> appeals = appealService.getAppealsByStatus(status);
        return ResponseEntity.ok(appeals);
    }

    /**
     * Get pending appeals count.
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<AppealCountResponse> getPendingCount() {
        log.info("Fetching pending appeals count");
        long count = appealService.getPendingAppealCount();
        return ResponseEntity.ok(AppealCountResponse.builder().pendingCount(count).build());
    }

    /**
     * Moderator review of an appeal.
     * Requires MODERATOR or ADMIN role.
     * Moderator provides review comments.
     */
    @PutMapping("/{appealId}/review")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<AppealDTO> moderatorReview(
            @PathVariable UUID appealId,
            @Valid @RequestBody ModeratorReviewRequest request) {
        log.info("Moderator reviewing appeal: {}", appealId);
        AppealDTO appeal = appealService.moderatorReview(appealId, request.getReview());
        return ResponseEntity.ok(appeal);
    }

    /**
     * Admin/Moderator decision on an appeal.
     * Requires MODERATOR or ADMIN role.
     * Approves (lifts ban/restores post) or rejects the appeal.
     */
    @PutMapping("/{appealId}/decision")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<AppealDTO> adminDecision(
            @PathVariable UUID appealId,
            @Valid @RequestBody AdminDecisionRequest request) {
        log.info("Moderator/Admin making decision on appeal: {}", appealId);
        AppealDTO appeal = appealService.adminDecision(appealId, request.getApproved(), request.getDecision());
        return ResponseEntity.ok(appeal);
    }

    /**
     * Request DTO for appeal submission.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AppealRequest {
        @NotNull(message = "Ban ID is required")
        private UUID banId;

        @NotBlank(message = "Reason is required")
        private String reason;
    }

    /**
     * Request DTO for post appeal submission.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PostAppealRequest {
        @NotNull(message = "Post ID is required")
        private UUID postId;

        @NotBlank(message = "Reason is required")
        private String reason;
    }

    /**
     * Request DTO for moderator review.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModeratorReviewRequest {
        @NotBlank(message = "Review is required")
        private String review;
    }

    /**
     * Request DTO for admin decision.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AdminDecisionRequest {
        @NotNull(message = "Decision is required")
        private Boolean approved;

        @NotBlank(message = "Decision reason is required")
        private String decision;
    }

    /**
     * Response DTO for appeal count.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class AppealCountResponse {
        private long pendingCount;
    }
}
