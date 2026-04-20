package com.example.socialapp.service.impl;

import com.example.socialapp.entity.ModerationLog;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.DecisionType;
import com.example.socialapp.enums.ModerationLogAction;
import com.example.socialapp.repository.ModerationLogRepository;
import com.example.socialapp.service.ModerationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Moderation Log Service Implementation.
 * Provides moderation action logging operations.
 */
@Slf4j
@Service
@Transactional
public class ModerationLogServiceImpl implements ModerationLogService {

    private final ModerationLogRepository moderationLogRepository;

    public ModerationLogServiceImpl(ModerationLogRepository moderationLogRepository) {
        this.moderationLogRepository = moderationLogRepository;
    }

    @Override
    public void logAction(User moderator, UUID postId, DecisionType action) {
        if (moderator == null || postId == null || action == null) {
            log.warn("Invalid moderation log parameters - moderator: {}, postId: {}, action: {}",
                moderator != null, postId, action);
            return;
        }

        // Convert DecisionType to ModerationLogAction
        ModerationLogAction logAction = convertDecisionTypeToAction(action);

        // Create and save moderation log
        ModerationLog logEntry = ModerationLog.builder()
            .moderatorId(moderator.getId())
            .moderatorName(moderator.getFirstName() + " " + moderator.getLastName())
            .action(logAction)
            .postId(postId)
            .build();

        moderationLogRepository.save(logEntry);
        log.info("Moderation action logged - Moderator: {}, Post: {}, Action: {}", 
            moderator.getId(), postId, logAction);
    }

    /**
     * Convert DecisionType to ModerationLogAction.
     *
     * @param decisionType the decision type
     * @return the corresponding moderation log action
     */
    private ModerationLogAction convertDecisionTypeToAction(DecisionType decisionType) {
        return switch (decisionType) {
            case APPROVED -> ModerationLogAction.APPROVED;
            case REMOVED -> ModerationLogAction.REMOVED;
            case ESCALATED -> ModerationLogAction.APPROVED; // Default to APPROVED for escalations
        };
    }
}
