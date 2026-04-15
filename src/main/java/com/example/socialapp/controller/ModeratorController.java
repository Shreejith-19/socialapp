package com.example.socialapp.controller;

import com.example.socialapp.dto.PostDTO;
import com.example.socialapp.entity.ModerationDecision;
import com.example.socialapp.enums.DecisionType;
import com.example.socialapp.enums.PostStatus;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.service.ModerationService;
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
import java.util.UUID;

/**
 * Moderator Controller.
 * Handles moderation endpoints for reviewing and deciding on flagged posts.
 * 
 * MODERATOR Role Endpoints (MODERATOR or ADMIN):
 * - GET /api/v1/moderation/queue - Get all flagged posts pending moderation
 * - POST /api/v1/moderation/decision - Make moderation decision (APPROVED, REMOVED, ESCALATED)
 * - GET /api/v1/moderation/decision/{postId} - Get moderation decision for a post
 * 
 * All endpoints in this controller require MODERATOR or ADMIN role.
 * Class-level @PreAuthorize enforces MODERATOR role for all methods.
 */
@Slf4j
@RestController
@RequestMapping("/v1/moderation")
@Validated
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class ModeratorController {

    private final ModerationService moderationService;
    private final PostRepository postRepository;

    public ModeratorController(ModerationService moderationService, PostRepository postRepository) {
        this.moderationService = moderationService;
        this.postRepository = postRepository;
    }

    /**
     * Get all flagged posts pending moderation.
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<PostDTO>> getModerationQueue(Pageable pageable) {
        log.info("Fetching moderation queue");

        // Get all flagged posts and convert to DTO
        Page<PostDTO> flaggedPosts = postRepository.findByStatus(PostStatus.FLAGGED, pageable)
            .map(this::mapToDTO);

        log.info("Returned {} flagged posts", flaggedPosts.getTotalElements());
        return ResponseEntity.ok(flaggedPosts);
    }

    /**
     * Make a moderation decision on a flagged post.
     * Requires MODERATOR or ADMIN role.
     * 
     * APPROVED: Publish the post and remove from queue
     * REMOVED: Delete the post and apply penalty to author
     * ESCALATED: Keep in queue for senior review
     */
    @PostMapping("/decision")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ModerationDecisionResponse> makeDecision(
            @Valid @RequestBody ModerationDecisionRequest request) {
        log.info("Making moderation decision for post: {}, Decision: {}", request.getPostId(), request.getDecisionType());

        // Validate post exists
        postRepository.findById(request.getPostId())
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", request.getPostId().toString()));

        // Make decision
        ModerationDecision decision = moderationService.makeDecision(
            request.getDecisionType(),
            request.getPostId(),
            request.getReason()
        );

        ModerationDecisionResponse response = ModerationDecisionResponse.builder()
            .decisionId(decision.getId())
            .postId(decision.getPost().getId())
            .decisionType(decision.getDecisionType())
            .reason(decision.getReason())
            .decidedAt(decision.getDecidedAt())
            .message(getDecisionMessage(decision.getDecisionType()))
            .build();

        log.info("Decision saved: {}", decision.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get moderation decision for a specific post.
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/decision/{postId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ModerationDecisionResponse> getDecision(@PathVariable UUID postId) {
        log.info("Fetching moderation decision for post: {}", postId);

        ModerationDecision decision = moderationService.getDecisionByPostId(postId)
            .orElseThrow(() -> new ResourceNotFoundException("ModerationDecision", "postId", postId.toString()));

        ModerationDecisionResponse response = ModerationDecisionResponse.builder()
            .decisionId(decision.getId())
            .postId(decision.getPost().getId())
            .decisionType(decision.getDecisionType())
            .reason(decision.getReason())
            .decidedAt(decision.getDecidedAt())
            .message(getDecisionMessage(decision.getDecisionType()))
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get count of pending moderation items.
     * Requires MODERATOR or ADMIN role.
     */
    @GetMapping("/queue/count")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<QueueCountResponse> getQueueCount() {
        log.info("Fetching moderation queue count");
        int count = moderationService.getPendingModerationCount();
        
        QueueCountResponse response = QueueCountResponse.builder()
            .pendingCount(count)
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to map Post entity to PostDTO.
     */
    private PostDTO mapToDTO(com.example.socialapp.entity.Post post) {
        return PostDTO.builder()
            .id(post.getId())
            .content(post.getContent())
            .status(post.getStatus())
            .authorId(post.getAuthor().getId())
            .authorUsername(post.getAuthor().getUsername())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }

    /**
     * Helper method to generate decision message.
     */
    private String getDecisionMessage(DecisionType decisionType) {
        return switch (decisionType) {
            case APPROVED -> "Post has been approved and published";
            case REMOVED -> "Post has been removed from the platform";
            case ESCALATED -> "Post has been escalated for senior review";
        };
    }

    /**
     * Request DTO for moderation decision.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ModerationDecisionRequest {
        @NotNull(message = "Post ID is required")
        private UUID postId;

        @NotNull(message = "Decision type is required")
        private DecisionType decisionType;

        // Reason is optional - moderators may not always provide detailed justification
        private String reason;
    }

    /**
     * Response DTO for moderation decision.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ModerationDecisionResponse {
        private UUID decisionId;
        private UUID postId;
        private DecisionType decisionType;
        private String reason;
        private java.time.LocalDateTime decidedAt;
        private String message;
    }

    /**
     * Response DTO for queue count.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class QueueCountResponse {
        private int pendingCount;
    }
}
