package com.example.socialapp.repository;

import com.example.socialapp.entity.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Moderation Log Repository.
 * Data access for moderation logs.
 */
@Repository
public interface ModerationLogRepository extends JpaRepository<ModerationLog, UUID> {

    /**
     * Find all moderation logs ordered by creation date (newest first).
     */
    List<ModerationLog> findAllByOrderByCreatedAtDesc();
}
