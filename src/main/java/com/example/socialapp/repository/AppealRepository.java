package com.example.socialapp.repository;

import com.example.socialapp.entity.Appeal;
import com.example.socialapp.entity.Ban;
import com.example.socialapp.enums.AppealStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Appeal Repository.
 * Data access for user appeals against bans and rejected posts.
 */
@Repository
public interface AppealRepository extends JpaRepository<Appeal, UUID> {

    /**
     * Find appeal by ban.
     */
    Optional<Appeal> findByBan(Ban ban);

    /**
     * Find appeal by ban ID.
     */
    Optional<Appeal> findByBanId(UUID banId);

    /**
     * Find appeal by post ID.
     */
    Optional<Appeal> findByPostId(UUID postId);

    /**
     * Find all appeals by status.
     */
    List<Appeal> findByStatus(AppealStatus status);

    /**
     * Find appeals by status with pagination.
     */
    Page<Appeal> findByStatus(AppealStatus status, Pageable pageable);

    /**
     * Find all pending appeals (moderator review queue) ordered by creation date.
     */
    List<Appeal> findByStatusOrderByCreatedAtAsc(AppealStatus status);

    /**
     * Find pending appeals with pagination.
     */
    Page<Appeal> findByStatusOrderByCreatedAtAsc(AppealStatus status, Pageable pageable);

    /**
     * Count appeals by status.
     */
    long countByStatus(AppealStatus status);

    /**
     * Check if appeal exists for ban.
     */
    boolean existsByBan(Ban ban);

    /**
     * Check if appeal exists for ban ID.
     */
    boolean existsByBanId(UUID banId);

    /**
     * Check if appeal exists for post ID.
     */
    boolean existsByPostId(UUID postId);
}
