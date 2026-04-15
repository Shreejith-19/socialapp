package com.example.socialapp.service;

import com.example.socialapp.dto.CommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Comment Service Interface.
 * Defines comment management operations.
 */
public interface CommentService {
    
    /**
     * Create a new comment on a post.
     *
     * @param authorId the UUID of the comment author
     * @param postId the UUID of the post being commented on
     * @param content the comment content
     * @return the created CommentDTO
     * @throws com.example.socialapp.exception.ResourceNotFoundException if user or post not found
     * @throws IllegalArgumentException if content is empty
     */
    CommentDTO createComment(UUID authorId, UUID postId, String content);
    
    /**
     * Get comment by ID.
     *
     * @param commentId the comment ID
     * @return the CommentDTO
     * @throws com.example.socialapp.exception.ResourceNotFoundException if comment not found
     */
    CommentDTO getCommentById(UUID commentId);
    
    /**
     * Get all comments for a post with pagination.
     *
     * @param postId the post UUID
     * @param pageable pagination info
     * @return page of CommentDTOs
     */
    Page<CommentDTO> getCommentsByPost(UUID postId, Pageable pageable);
    
    /**
     * Get all comments by a user with pagination.
     *
     * @param authorId the author UUID
     * @param pageable pagination info
     * @return page of CommentDTOs
     */
    Page<CommentDTO> getCommentsByAuthor(UUID authorId, Pageable pageable);
    
    /**
     * Update (edit) a comment.
     *
     * @param commentId the comment ID
     * @param newContent the new content
     * @param userId the ID of the user making the update (for authorization)
     * @return the updated CommentDTO
     * @throws com.example.socialapp.exception.ResourceNotFoundException if comment not found
     * @throws IllegalArgumentException if user is not the comment author or content is empty
     */
    CommentDTO updateComment(UUID commentId, String newContent, UUID userId);
    
    /**
     * Delete a comment.
     *
     * @param commentId the comment ID
     * @param userId the ID of the user making the delete (for authorization)
     * @throws com.example.socialapp.exception.ResourceNotFoundException if comment not found
     * @throws IllegalArgumentException if user is not the comment author or admin
     */
    void deleteComment(UUID commentId, UUID userId);
    
    /**
     * Get comment count for a post.
     *
     * @param postId the post ID
     * @return number of comments
     */
    long getCommentCount(UUID postId);
}
