package com.example.socialapp.service.impl;

import com.example.socialapp.entity.Ban;
import com.example.socialapp.entity.ModerationDecision;
import com.example.socialapp.entity.Post;
import com.example.socialapp.enums.BanType;
import com.example.socialapp.enums.DecisionType;
import com.example.socialapp.enums.PostStatus;
import com.example.socialapp.enums.UserStatus;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.queue.ModerationQueue;
import com.example.socialapp.repository.BanRepository;
import com.example.socialapp.repository.ModerationDecisionRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.ModerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Moderation Service Implementation.
 * Provides content moderation logic using NLP simulation and keyword filtering.
 */
@Slf4j
@Service
@Transactional
public class ModerationServiceImpl implements ModerationService {

    private final ModerationQueue moderationQueue;
    private final PostRepository postRepository;
    private final ModerationDecisionRepository moderationDecisionRepository;
    private final BanRepository banRepository;
    private final UserRepository userRepository;
    
    /**
     * Banned keywords that indicate inappropriate content.
     * In production, this would be replaced with a real NLP model (BERT, etc.)
     */
    private static final Set<String> BANNED_KEYWORDS = new HashSet<>(Arrays.asList(
        "spam", "abuse", "hate", "violence", "explicit", "offensive",
        "harassment", "threat", "scam", "malware", "phishing",
        "discriminate", "racist", "sexist", "inappropriate", "vulgar",
        "illegal", "forbidden", "blocked", "restricted", "banned"
    ));

    /**
     * Suspicious patterns that indicate potential issues.
     */
    private static final Pattern[] SUSPICIOUS_PATTERNS = {
        Pattern.compile("(http|https)://[^\\s]+", Pattern.CASE_INSENSITIVE), // URLs
        Pattern.compile("@[a-zA-Z0-9_]+", Pattern.CASE_INSENSITIVE), // Mentions at scale
        Pattern.compile("[A-Z]{5,}", Pattern.CASE_INSENSITIVE), // ALL CAPS words
    };

    public ModerationServiceImpl(ModerationQueue moderationQueue, PostRepository postRepository,
                                ModerationDecisionRepository moderationDecisionRepository,
                                BanRepository banRepository, UserRepository userRepository) {
        this.moderationQueue = moderationQueue;
        this.postRepository = postRepository;
        this.moderationDecisionRepository = moderationDecisionRepository;
        this.banRepository = banRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean reviewContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.warn("Content review requested for empty or null content");
            return true; // Empty content is safe
        }

        log.debug("Reviewing content for moderation: {}", content.substring(0, Math.min(50, content.length())));

        // Convert to lowercase for case-insensitive matching
        String lowerContent = content.toLowerCase();

        // Check for banned keywords
        for (String keyword : BANNED_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                log.warn("Banned keyword detected in content: {}", keyword);
                return false; // Content contains banned keyword
            }
        }

        // Check for suspicious patterns
        int suspiciousCount = 0;
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                suspiciousCount++;
            }
        }

        // Flag if too many suspicious patterns detected
        if (suspiciousCount > 2) {
            log.warn("Multiple suspicious patterns detected in content. Count: {}", suspiciousCount);
            return false;
        }

        log.debug("Content review completed: APPROVED");
        return true; // Content is clean
    }

    @Override
    public void flagPost(Post post) {
        if (post == null) {
            log.warn("Attempted to flag null post");
            return;
        }

        log.info("Flagging post: {} for moderation", post.getId());
        post.markFlagged();
        postRepository.save(post);
        log.info("Post flagged successfully: {}", post.getId());
    }

    @Override
    public void sendToQueue(Post post) {
        if (post == null) {
            log.warn("Attempted to send null post to moderation queue");
            return;
        }

        log.info("Sending post to moderation queue: {}", post.getId());
        
        // Ensure post is flagged before queuing
        if (post.getStatus() != PostStatus.FLAGGED) {
            post.markFlagged();
            postRepository.save(post);
        }

        // Add to moderation queue
        moderationQueue.addPost(post);
        log.info("Post queued successfully for moderation: {}. Queue size: {}", 
                 post.getId(), moderationQueue.getQueueSize());
    }

    @Override
    @Transactional(readOnly = true)
    public int getPendingModerationCount() {
        return moderationQueue.getQueueSize();
    }

    @Override
    public void approvePost(Post post) {
        if (post == null) {
            log.warn("Attempted to approve null post");
            return;
        }

        log.info("Approving post: {}", post.getId());
        
        // Remove from moderation queue by ID to avoid lazy initialization issues
        moderationQueue.removePostById(post.getId());
        
        // Keep FLAGGED status or revert to PUBLISHED
        post.publish();
        postRepository.save(post);
        
        log.info("Post approved successfully: {}", post.getId());
    }

    @Override
    public void rejectPost(Post post) {
        if (post == null) {
            log.warn("Attempted to reject null post");
            return;
        }

        log.info("Rejecting post: {}", post.getId());
        
        // Remove from moderation queue by ID to avoid lazy initialization issues
        moderationQueue.removePostById(post.getId());
        
        // Mark as removed
        post.remove();
        postRepository.save(post);
        
        log.info("Post rejected successfully: {}", post.getId());
    }

    @Override
    public ModerationDecision makeDecision(DecisionType decisionType, UUID postId, String reason) {
        log.info("Making moderation decision for post: {}, Decision: {}", postId, decisionType);

        // Load post
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        // Create decision record
        ModerationDecision decision = ModerationDecision.builder()
            .post(post)
            .decisionType(decisionType)
            .reason(reason)
            .build();

        // Process decision based on type
        switch (decisionType) {
            case APPROVED:
                log.info("Approving post: {}", postId);
                // Remove from queue by ID to avoid lazy initialization issues
                moderationQueue.removePostById(postId);
                // Set to PUBLISHED
                post.publish();
                postRepository.save(post);
                log.info("Post published: {}", postId);
                break;

            case REMOVED:
                log.info("Removing post: {}", postId);
                // Remove from queue by ID to avoid lazy initialization issues
                moderationQueue.removePostById(postId);
                // Mark as removed
                post.remove();
                postRepository.save(post);
                
                // Apply penalty to author
                if (post.getAuthor() != null) {
                    post.getAuthor().setStatus(UserStatus.TEMP_BANNED);
                    
                    // Create temporary ban record (7 days)
                    Ban tempBan = Ban.builder()
                        .user(post.getAuthor())
                        .type(BanType.TEMPORARY)
                        .startDate(LocalDateTime.now())
                        .endDate(LocalDateTime.now().plusDays(7))
                        .reason("Post removed: " + reason)
                        .build();
                    banRepository.save(tempBan);
                    
                    userRepository.save(post.getAuthor());
                    log.warn("User temporarily banned due to post removal: {}. Ban expiry: 7 days", post.getAuthor().getId());
                }
                log.info("Post removed: {}", postId);
                break;

            case ESCALATED:
                log.info("Escalating post for senior review: {}", postId);
                // Keep in queue, do nothing else
                break;

            default:
                log.warn("Unknown decision type: {}", decisionType);
        }

        // Save decision
        ModerationDecision savedDecision = moderationDecisionRepository.save(decision);
        log.info("Moderation decision saved: {}", savedDecision.getId());

        return savedDecision;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModerationDecision> getDecisionByPostId(UUID postId) {
        log.debug("Retrieving decision for post: {}", postId);
        return moderationDecisionRepository.findByPostId(postId);
    }
}
