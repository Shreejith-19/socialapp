package com.example.socialapp.repository;

import com.example.socialapp.entity.ModerationDecision;
import com.example.socialapp.entity.Post;
import com.example.socialapp.enums.DecisionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Moderation Decision Repository.
 * Data access for moderation decisions.
 */
@Repository
public interface ModerationDecisionRepository extends JpaRepository<ModerationDecision, UUID> {

    /**
     * Find decision by post.
     */
    Optional<ModerationDecision> findByPost(Post post);

    /**
     * Find decision by post ID.
     */
    Optional<ModerationDecision> findByPostId(UUID postId);

    /**
     * Find all decisions by decision type with pagination.
     */
    Page<ModerationDecision> findByDecisionType(DecisionType decisionType, Pageable pageable);

    /**
     * Count decisions by decision type.
     */
    long countByDecisionType(DecisionType decisionType);

    /**
     * Check if decision exists for post.
     */
    boolean existsByPost(Post post);

    /**
     * Check if decision exists for post ID.
     */
    boolean existsByPostId(UUID postId);
}
