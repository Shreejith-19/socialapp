package com.example.socialapp.controller;

import com.example.socialapp.dto.CommentDTO;
import com.example.socialapp.entity.User;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.CommentService;
import com.example.socialapp.util.SecurityUtil;
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
import java.util.UUID;

/**
 * Comment Controller.
 * Handles comment creation, retrieval, editing, and deletion HTTP requests with role-based access control.
 * 
 * USER Role Endpoints:
 * - POST /api/v1/comments - Create a comment on a post
 * - GET /api/v1/comments/{id} - Get comment by ID
 * - GET /api/v1/comments/post/{postId} - Get all comments for a post (paginated)
 * - GET /api/v1/comments/author/{authorId} - Get all comments by a user (paginated)
 * - PUT /api/v1/comments/{id} - Edit own comment
 * - DELETE /api/v1/comments/{id} - Delete own comment
 * - GET /api/v1/comments/count/{postId} - Get comment count for post
 * 
 * Authorization: Users can only edit/delete their own comments. Service enforces this.
 */
@Slf4j
@RestController
@RequestMapping("/v1/comments")
@Validated
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    public CommentController(CommentService commentService, UserRepository userRepository) {
        this.commentService = commentService;
        this.userRepository = userRepository;
    }

    /**
     * Create a new comment on a post.
     * User must be authenticated with USER role.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<CommentDTO> createComment(@Valid @RequestBody CommentRequest commentRequest) {
        log.info("Comment creation request received for post: {}", commentRequest.getPostId());
        
        // Get current authenticated user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        CommentDTO createdComment = commentService.createComment(
            currentUser.getId(),
            commentRequest.getPostId(),
            commentRequest.getContent()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    /**
     * Get comment by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<CommentDTO> getCommentById(@PathVariable UUID id) {
        log.info("Fetching comment with ID: {}", id);
        CommentDTO comment = commentService.getCommentById(id);
        return ResponseEntity.ok(comment);
    }

    /**
     * Get all comments for a specific post with pagination.
     */
    @GetMapping("/post/{postId}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<CommentDTO>> getCommentsByPost(
            @PathVariable UUID postId,
            Pageable pageable) {
        log.info("Fetching comments for post: {}", postId);
        Page<CommentDTO> comments = commentService.getCommentsByPost(postId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * Get all comments by a specific author with pagination.
     */
    @GetMapping("/author/{authorId}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<CommentDTO>> getCommentsByAuthor(
            @PathVariable UUID authorId,
            Pageable pageable) {
        log.info("Fetching comments by author: {}", authorId);
        Page<CommentDTO> comments = commentService.getCommentsByAuthor(authorId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * Update (edit) a comment.
     * User must be authenticated and be the comment author.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentUpdateRequest updateRequest) {
        log.info("Comment update request for ID: {}", id);
        
        // Get current authenticated user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        CommentDTO updatedComment = commentService.updateComment(
            id,
            updateRequest.getContent(),
            currentUser.getId()
        );
        return ResponseEntity.ok(updatedComment);
    }

    /**
     * Delete a comment.
     * User must be authenticated and be the comment author.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID id) {
        log.info("Delete comment request for ID: {}", id);
        
        // Get current authenticated user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        commentService.deleteComment(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get comment count for a post.
     */
    @GetMapping("/count/{postId}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Long> getCommentCount(@PathVariable UUID postId) {
        log.info("Fetching comment count for post: {}", postId);
        long count = commentService.getCommentCount(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * Inner class for comment creation request.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentRequest {
        @NotBlank(message = "Post ID cannot be empty")
        private UUID postId;

        @NotBlank(message = "Comment content cannot be empty")
        private String content;
    }

    /**
     * Inner class for comment update request.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CommentUpdateRequest {
        @NotBlank(message = "Comment content cannot be empty")
        private String content;
    }
}
