package com.example.socialapp.repository;

import com.example.socialapp.entity.Ban;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.BanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ban Repository.
 * Data access for user bans.
 */
@Repository
public interface BanRepository extends JpaRepository<Ban, UUID> {

    /**
     * Find all bans for a user.
     */
    List<Ban> findByUser(User user);

    /**
     * Find all active bans for a user (PERMANENT or TEMPORARY not expired).
     */
    @Query("SELECT b FROM Ban b WHERE b.user = :user AND " +
           "(b.type = 'PERMANENT' OR (b.type = 'TEMPORARY' AND b.endDate > CURRENT_TIMESTAMP))")
    List<Ban> findActiveBansByUser(@Param("user") User user);

    /**
     * Find latest active ban for a user.
     */
    @Query("SELECT b FROM Ban b WHERE b.user = :user AND " +
           "(b.type = 'PERMANENT' OR (b.type = 'TEMPORARY' AND b.endDate > CURRENT_TIMESTAMP)) " +
           "ORDER BY b.createdAt DESC LIMIT 1")
    Optional<Ban> findLatestActiveBanByUser(@Param("user") User user);

    /**
     * Find bans for a user with pagination.
     */
    Page<Ban> findByUser(User user, Pageable pageable);

    /**
     * Find bans by user ID.
     */
    List<Ban> findByUserId(UUID userId);

    /**
     * Find active bans by user ID.
     */
    @Query("SELECT b FROM Ban b WHERE b.user.id = :userId AND " +
           "(b.type = 'PERMANENT' OR (b.type = 'TEMPORARY' AND b.endDate > CURRENT_TIMESTAMP))")
    List<Ban> findActiveBansByUserId(@Param("userId") UUID userId);

    /**
     * Find bans by type.
     */
    List<Ban> findByType(BanType type);

    /**
     * Count active bans for a user.
     */
    @Query("SELECT COUNT(b) FROM Ban b WHERE b.user = :user AND " +
           "(b.type = 'PERMANENT' OR (b.type = 'TEMPORARY' AND b.endDate > CURRENT_TIMESTAMP))")
    long countActiveBansByUser(@Param("user") User user);

    /**
     * Check if user has any active ban.
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Ban b WHERE b.user = :user AND " +
           "(b.type = 'PERMANENT' OR (b.type = 'TEMPORARY' AND b.endDate > CURRENT_TIMESTAMP))")
    boolean hasActiveBan(@Param("user") User user);

    /**
     * Find temporary bans that have expired (for cleanup).
     */
    @Query("SELECT b FROM Ban b WHERE b.type = 'TEMPORARY' AND b.endDate < CURRENT_TIMESTAMP")
    List<Ban> findExpiredTemporaryBans();
}
