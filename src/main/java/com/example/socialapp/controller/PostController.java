package com.example.socialapp.controller;

import com.example.socialapp.dto.LikeDTO;
import com.example.socialapp.dto.PostDTO;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.entity.User;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.LikeService;
import com.example.socialapp.service.PostService;
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
 * Post Controller.
 * Handles post creation, retrieval, and interaction HTTP requests with role-based access control.
 * 
 * USER Role Endpoints:
 * - POST /api/v1/posts - Create a new post
 * - POST /api/v1/posts/{postId}/like - Like a post
 * - POST /api/v1/posts/{postId}/dislike - Dislike a post
 * - DELETE /api/v1/posts/{postId}/like - Remove like/dislike
 * - GET /api/v1/posts/{id} - Get post by ID
 * - GET /api/v1/posts/author/{authorId} - Get posts by author
 * - GET /api/v1/posts - Get all published posts
 * - GET /api/v1/posts/{postId}/like-count - Get like count
 * - GET /api/v1/posts/{postId}/dislike-count - Get dislike count
 * 
 * ADMIN Role Endpoints:
 * - DELETE /api/v1/posts/{id} - Delete any post
 */
@Slf4j
@RestController
@RequestMapping("/v1/posts")
@Validated
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;
    private final LikeService likeService;

    public PostController(PostService postService, UserRepository userRepository, LikeService likeService) {
        this.postService = postService;
        this.userRepository = userRepository;
        this.likeService = likeService;
    }

    /**
     * Create a new post.
     * User must be authenticated with USER role.
     * Content is automatically reviewed and either published or flagged.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody PostRequest postRequest) {
        log.info("Post creation request received");
        
        // Get current authenticated user's email
        String userEmail = SecurityUtil.getCurrentUserEmail();
        log.debug("Creating post for user: {}", userEmail);
        
        // Look up user UUID from database
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        UUID authorId = currentUser.getId();
        
        PostDTO createdPost = postService.createPost(authorId, postRequest.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    /**
     * Get post by ID.
     * Public endpoint - any authenticated user can view posts.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<PostDTO> getPostById(@PathVariable UUID id) {
        log.info("Fetching post with ID: {}", id);
        PostDTO post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    /**
     * Get all posts by a specific author with pagination.
     * Public endpoint - any authenticated user can view author posts.
     */
    @GetMapping("/author/{authorId}")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<PostDTO>> getPostsByAuthor(
            @PathVariable UUID authorId,
            Pageable pageable) {
        log.info("Fetching posts by author: {}", authorId);
        Page<PostDTO> posts = postService.getPostsByAuthor(authorId, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get all posts created by the authenticated user.
     * Includes all post statuses (PUBLISHED, FLAGGED, REMOVED) for the user's own posts.
     * Only the authenticated user can view their own posts (security enforced at service level).
     * 
     * @param pageable pagination parameters
     * @return page of user's posts with moderation status
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<PostDTO>> getMyPosts(Pageable pageable) {
        // Get current authenticated user's email
        String userEmail = SecurityUtil.getCurrentUserEmail();
        
        // Security check: ensure user is authenticated
        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("Attempted to access /posts/my without valid authentication");
            throw new IllegalStateException("User must be authenticated to access their posts");
        }
        
        log.info("Fetching all posts for authenticated user: {}", userEmail);
        
        // Look up user UUID from database - this ensures user exists and is active
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> {
                log.error("Authenticated user not found in database: {}", userEmail);
                return new ResourceNotFoundException("User", "email", userEmail);
            });
        UUID userId = currentUser.getId();
        
        log.debug("User {} has ID: {}", userEmail, userId);
        
        // Fetch all posts created by this user (including flagged and removed)
        Page<PostDTO> userPosts = postService.getMyPosts(userId, pageable);
        
        log.info("Retrieved {} posts for user: {}", userPosts.getTotalElements(), userEmail);
        return ResponseEntity.ok(userPosts);
    }

    /**
     * Get all published posts with pagination.
     * Public endpoint - any authenticated user can view published posts.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Page<PostDTO>> getAllPublishedPosts(Pageable pageable) {
        log.info("Fetching all published posts");
        Page<PostDTO> posts = postService.getAllPublishedPosts(pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * Delete a post.
     * Only admin can delete posts (can be extended to allow author as well).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id) {
        log.info("Delete post request for ID: {}", id);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Like a post.
     * User must be authenticated with USER role.
     */
    @PostMapping("/{postId}/like")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<LikeDTO> likePost(@PathVariable UUID postId) {
        log.info("Like request for post: {}", postId);
        
        // Get current authenticated user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        LikeDTO likeDTO = likeService.likePost(currentUser.getId(), postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(likeDTO);
    }

    /**
     * Dislike a post.
     * User must be authenticated with USER role.
     */
    @PostMapping("/{postId}/dislike")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<LikeDTO> dislikePost(@PathVariable UUID postId) {
        log.info("Dislike request for post: {}", postId);
        
        // Get current authenticated user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        LikeDTO likeDTO = likeService.dislikePost(currentUser.getId(), postId);
        return ResponseEntity.status(HttpStatus.CREATED).body(likeDTO);
    }

    /**
     * Remove a like/dislike for a post.
     * User must be authenticated with USER role.
     */
    @DeleteMapping("/{postId}/like")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> removeLike(@PathVariable UUID postId) {
        log.info("Remove like request for post: {}", postId);
        
        // Get current authenticated user
        String userEmail = SecurityUtil.getCurrentUserEmail();
        User currentUser = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        
        likeService.removeLike(currentUser.getId(), postId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get like count for a post.
     * Public endpoint - any authenticated user can view like counts.
     */
    @GetMapping("/{postId}/like-count")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Long> getLikeCount(@PathVariable UUID postId) {
        log.info("Fetching like count for post: {}", postId);
        long count = likeService.getLikeCount(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get dislike count for a post.
     * Public endpoint - any authenticated user can view dislike counts.
     */
    @GetMapping("/{postId}/dislike-count")
    @PreAuthorize("hasAnyRole('USER', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<Long> getDislikeCount(@PathVariable UUID postId) {
        log.info("Fetching dislike count for post: {}", postId);
        long count = likeService.getDislikeCount(postId);
        return ResponseEntity.ok(count);
    }

    /**
     * Inner class for post creation request.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PostRequest {
        @NotBlank(message = "Post content cannot be empty")
        private String content;
    }
}
