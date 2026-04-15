package com.example.socialapp.service;

import com.example.socialapp.dto.LikeDTO;
import java.util.UUID;

/**
 * Like Service Interface.
 * Defines like/dislike management operations.
 */
public interface LikeService {
    
    /**
     * Like a post.
     *
     * @param userId the UUID of the user liking the post
     * @param postId the UUID of the post being liked
     * @return the created or updated LikeDTO
     * @throws com.example.socialapp.exception.ResourceNotFoundException if user or post not found
     */
    LikeDTO likePost(UUID userId, UUID postId);
    
    /**
     * Dislike a post.
     *
     * @param userId the UUID of the user disliking the post
     * @param postId the UUID of the post being disliked
     * @return the created or updated LikeDTO
     * @throws com.example.socialapp.exception.ResourceNotFoundException if user or post not found
     */
    LikeDTO dislikePost(UUID userId, UUID postId);
    
    /**
     * Remove a like/dislike for a post.
     *
     * @param userId the UUID of the user
     * @param postId the UUID of the post
     * @throws com.example.socialapp.exception.ResourceNotFoundException if like not found
     */
    void removeLike(UUID userId, UUID postId);
    
    /**
     * Get like count for a post.
     *
     * @param postId the UUID of the post
     * @return number of likes
     */
    long getLikeCount(UUID postId);
    
    /**
     * Get dislike count for a post.
     *
     * @param postId the UUID of the post
     * @return number of dislikes
     */
    long getDislikeCount(UUID postId);
    
    /**
     * Check if a user has liked a post.
     *
     * @param userId the UUID of the user
     * @param postId the UUID of the post
     * @return true if user has liked the post and it is marked as like
     */
    boolean hasUserLiked(UUID userId, UUID postId);
    
    /**
     * Check if a user has disliked a post.
     *
     * @param userId the UUID of the user
     * @param postId the UUID of the post
     * @return true if user has liked the post and it is marked as dislike
     */
    boolean hasUserDisliked(UUID userId, UUID postId);
}
