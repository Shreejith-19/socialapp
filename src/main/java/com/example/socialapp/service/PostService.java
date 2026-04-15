package com.example.socialapp.service;

import com.example.socialapp.dto.PostDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Post Service Interface.
 * Defines post creation and management operations.
 */
public interface PostService {
    
    /**
     * Create a new post with automatic moderation.
     * Flow: User → canPost check → Moderation review → Publish or Flag
     *
     * @param authorId the UUID of the post author
     * @param content the post content
     * @return the created PostDTO
     * @throws IllegalArgumentException if author cannot post (banned)
     */
    PostDTO createPost(UUID authorId, String content);
    
    /**
     * Get post by ID.
     *
     * @param postId the post ID
     * @return the PostDTO
     */
    PostDTO getPostById(UUID postId);
    
    /**
     * Get all posts by author with pagination.
     *
     * @param authorId the author UUID
     * @param pageable pagination info
     * @return page of PostDTOs
     */
    Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable);
    
    /**
     * Get all posts created by the authenticated user (including flagged and removed posts).
     * This includes all post statuses (PUBLISHED, FLAGGED, REMOVED).
     *
     * @param userId the authenticated user's UUID
     * @param pageable pagination info
     * @return page of PostDTOs with all statuses
     */
    Page<PostDTO> getMyPosts(UUID userId, Pageable pageable);
    
    /**
     * Get all published posts with pagination.
     *
     * @param pageable pagination info
     * @return page of PostDTOs
     */
    Page<PostDTO> getAllPublishedPosts(Pageable pageable);
    
    /**
     * Delete a post (author or admin only).
     *
     * @param postId the post ID to delete
     */
    void deletePost(UUID postId);
}
