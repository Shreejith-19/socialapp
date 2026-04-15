package com.example.socialapp.repository;

import com.example.socialapp.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Like Repository.
 * Provides CRUD and query operations for Like entity.
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, UUID> {
    
    /**
     * Find a like by user ID and post ID.
     *
     * @param userId the user ID
     * @param postId the post ID
     * @return Optional containing the Like if found
     */
    Optional<Like> findByUserIdAndPostId(UUID userId, UUID postId);
    
    /**
     * Count total likes for a specific post.
     *
     * @param postId the post ID
     * @return number of likes
     */
    long countByPostIdAndIsLikeTrue(UUID postId);
    
    /**
     * Count total dislikes for a specific post.
     *
     * @param postId the post ID
     * @return number of dislikes
     */
    long countByPostIdAndIsLikeFalse(UUID postId);
    
    /**
     * Delete a like by user ID and post ID.
     *
     * @param userId the user ID
     * @param postId the post ID
     */
    void deleteByUserIdAndPostId(UUID userId, UUID postId);
}
