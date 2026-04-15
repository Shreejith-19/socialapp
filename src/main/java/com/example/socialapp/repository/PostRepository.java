package com.example.socialapp.repository;

import com.example.socialapp.entity.Post;
import com.example.socialapp.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Post Repository.
 * Provides CRUD and query operations for Post entity.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    /**
     * Find all posts by author ID.
     */
    List<Post> findByAuthorId(UUID authorId);
    
    /**
     * Find all posts by author ID with pagination.
     */
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
    
    /**
     * Find all posts with a specific status with pagination.
     */
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    
    /**
     * Find all posts with a specific status.
     */
    List<Post> findByStatus(PostStatus status);
    
    /**
     * Count posts by author ID.
     */
    long countByAuthorId(UUID authorId);
    
    /**
     * Count posts by status.
     */
    long countByStatus(PostStatus status);
}
