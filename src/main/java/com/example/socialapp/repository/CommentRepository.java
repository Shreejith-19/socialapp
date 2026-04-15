package com.example.socialapp.repository;

import com.example.socialapp.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Comment Repository.
 * Provides CRUD and query operations for Comment entity.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    /**
     * Find all comments by post ID.
     *
     * @param postId the post ID
     * @return list of comments for the post
     */
    List<Comment> findByPostId(UUID postId);
    
    /**
     * Find all comments by post ID with pagination.
     *
     * @param postId the post ID
     * @param pageable pagination info
     * @return page of comments
     */
    Page<Comment> findByPostId(UUID postId, Pageable pageable);
    
    /**
     * Find all comments by author ID.
     *
     * @param authorId the author UUID
     * @return list of comments by the author
     */
    List<Comment> findByAuthorId(UUID authorId);
    
    /**
     * Find all comments by author ID with pagination.
     *
     * @param authorId the author UUID
     * @param pageable pagination info
     * @return page of comments
     */
    Page<Comment> findByAuthorId(UUID authorId, Pageable pageable);
    
    /**
     * Count comments for a specific post.
     *
     * @param postId the post ID
     * @return number of comments
     */
    long countByPostId(UUID postId);
}
