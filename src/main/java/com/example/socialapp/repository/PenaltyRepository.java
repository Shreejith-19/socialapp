package com.example.socialapp.repository;

import com.example.socialapp.entity.Penalty;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.PenaltyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Penalty Repository.
 * Data access for user penalties.
 */
@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, UUID> {

    /**
     * Find all penalties for a user.
     */
    List<Penalty> findByUser(User user);

    /**
     * Find all penalties for a user with pagination.
     */
    Page<Penalty> findByUser(User user, Pageable pageable);

    /**
     * Find penalties by user ID.
     */
    List<Penalty> findByUserId(UUID userId);

    /**
     * Find penalties by user ID with pagination.
     */
    Page<Penalty> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find penalties by type.
     */
    List<Penalty> findByType(PenaltyType type);

    /**
     * Find penalties by type with pagination.
     */
    Page<Penalty> findByType(PenaltyType type, Pageable pageable);

    /**
     * Find penalties by user and type.
     */
    List<Penalty> findByUserAndType(User user, PenaltyType type);

    /**
     * Count penalties for a user.
     */
    long countByUser(User user);

    /**
     * Count penalties for a user by type.
     */
    long countByUserAndType(User user, PenaltyType type);

    /**
     * Sum total points for a user.
     */
    @Query("SELECT COALESCE(SUM(p.points), 0) FROM Penalty p WHERE p.user = :user")
    Integer sumPointsByUser(@Param("user") User user);
}
