package com.example.socialapp.service.impl;

import com.example.socialapp.dto.AppealDTO;
import com.example.socialapp.entity.Appeal;
import com.example.socialapp.entity.Ban;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.AppealStatus;
import com.example.socialapp.enums.AppealType;
import com.example.socialapp.enums.NotificationType;
import com.example.socialapp.enums.PostStatus;
import com.example.socialapp.enums.UserStatus;
import com.example.socialapp.exception.ConflictException;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.AppealRepository;
import com.example.socialapp.repository.BanRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.AppealService;
import com.example.socialapp.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Appeal Service Implementation.
 * Provides appeal management logic for both ban appeals and post appeals
 * with moderator review and admin decision workflow.
 */
@Slf4j
@Service
@Transactional
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final BanRepository banRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AppealServiceImpl(AppealRepository appealRepository, BanRepository banRepository,
                           PostRepository postRepository, UserRepository userRepository,
                           NotificationService notificationService) {
        this.appealRepository = appealRepository;
        this.banRepository = banRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public AppealDTO submitAppeal(UUID banId, String reason) {
        log.info("User submitting appeal for ban: {}", banId);

        // Verify ban exists
        Ban ban = banRepository.findById(banId)
            .orElseThrow(() -> new ResourceNotFoundException("Ban", "id", banId.toString()));

        // Check if appeal already exists for this ban
        if (appealRepository.existsByBanId(banId)) {
            log.warn("Appeal already exists for ban: {}", banId);
            throw new ConflictException("An appeal for this ban already exists");
        }

        // Verify ban is active
        if (!ban.isActive()) {
            log.warn("Cannot appeal inactive ban: {}", banId);
            throw new IllegalStateException("Cannot appeal an inactive ban");
        }

        // Create appeal
        Appeal appeal = Appeal.builder()
            .appealType(AppealType.BAN)
            .ban(ban)
            .reason(reason)
            .status(AppealStatus.PENDING)
            .build();

        Appeal savedAppeal = appealRepository.save(appeal);
        log.info("Ban appeal submitted: {} for ban: {}", savedAppeal.getId(), banId);

        return mapToDTO(savedAppeal);
    }

    @Override
    public AppealDTO submitPostAppeal(UUID postId, String reason) {
        log.info("User submitting appeal for post: {}", postId);

        // Verify post exists
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        // Check if post is in REMOVED status
        if (post.getStatus() != PostStatus.REMOVED) {
            log.warn("Cannot appeal post with status: {}. Only REMOVED posts can be appealed.", post.getStatus());
            throw new IllegalStateException("Only posts with REMOVED status can be appealed");
        }

        // Check if appeal already exists for this post
        if (appealRepository.existsByPostId(postId)) {
            log.warn("Appeal already exists for post: {}", postId);
            throw new ConflictException("An appeal for this post already exists");
        }

        // Get current user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();  // getName() returns email
        
        User currentUser = userRepository.findByEmail(currentEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", currentEmail));

        // Verify user is the post author
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to appeal post {} they do not own", currentEmail, postId);
            throw new ConflictException("You can only appeal posts you authored");
        }

        // Create appeal
        Appeal appeal = Appeal.builder()
            .appealType(AppealType.POST)
            .post(post)
            .reason(reason)
            .status(AppealStatus.PENDING)
            .build();

        Appeal savedAppeal = appealRepository.save(appeal);
        log.info("Post appeal submitted: {} for post: {}", savedAppeal.getId(), postId);

        return mapToDTO(savedAppeal);
    }

    @Override
    @Transactional(readOnly = true)
    public AppealDTO getAppealById(UUID appealId) {
        log.debug("Fetching appeal: {}", appealId);
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal", "id", appealId.toString()));
        return mapToDTO(appeal);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppealDTO> getAppealByBanId(UUID banId) {
        log.debug("Fetching appeal by ban ID: {}", banId);
        return appealRepository.findByBanId(banId).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppealDTO> getAppealByPostId(UUID postId) {
        log.debug("Fetching appeal by post ID: {}", postId);
        return appealRepository.findByPostId(postId).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppealDTO> getAppealsByStatus(AppealStatus status) {
        log.info("Fetching appeals with status: {}", status);
        return appealRepository.findByStatus(status).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppealDTO> getPendingAppeals() {
        log.info("Fetching pending appeals for moderator review");
        return appealRepository.findByStatusOrderByCreatedAtAsc(AppealStatus.PENDING).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public AppealDTO moderatorReview(UUID appealId, String review) {
        log.info("Moderator reviewing appeal: {}", appealId);

        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal", "id", appealId.toString()));

        // Verify appeal is pending
        if (!appeal.isPending()) {
            log.warn("Cannot review non-pending appeal: {}", appealId);
            throw new IllegalStateException("Appeal is not pending review");
        }

        // Store moderator review
        appeal.setModeratorReview(review);
        
        // Keep status as PENDING - awaiting admin decision
        Appeal updatedAppeal = appealRepository.save(appeal);
        log.info("Moderator review added to appeal: {}", appealId);

        return mapToDTO(updatedAppeal);
    }

    @Override
    public AppealDTO adminDecision(UUID appealId, boolean approved, String decision) {
        log.info("Admin making decision on appeal: {}, approved={}", appealId, approved);

        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new ResourceNotFoundException("Appeal", "id", appealId.toString()));

        // Verify appeal is pending
        if (!appeal.isPending()) {
            log.warn("Cannot decide on non-pending appeal: {}", appealId);
            throw new IllegalStateException("Appeal is not pending decision");
        }

        // Set decision and status
        appeal.setAdminDecision(decision);
        
        if (approved) {
            log.info("Appeal APPROVED: {}", appealId);
            appeal.setStatus(AppealStatus.APPROVED);
            
            // Handle approval based on appeal type
            if (appeal.getAppealType() == AppealType.BAN) {
                // Lift the ban - set user status back to ACTIVE
                Ban ban = appeal.getBan();
                UUID userId = ban.getUser().getId();
                
                // Step 1: Clear the appeal's ban reference (break foreign key constraint)
                appeal.setBan(null);
                appealRepository.save(appeal);
                log.info("Appeal ban reference cleared for appeal: {}", appealId);
                
                // Step 2: Delete the ban record from database
                banRepository.delete(ban);
                banRepository.flush();  // Force deletion to complete
                log.info("Ban record deleted for user: {}", userId);
                
                // Step 3: Reload user from database to get fresh instance
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
                
                // Step 4: Set user status back to ACTIVE
                user.setStatus(UserStatus.ACTIVE);
                User savedUser = userRepository.save(user);
                log.info("Ban lifted for user: {}. User status set to ACTIVE. Ban deleted from database.", userId);
                
                // Step 5: Send notification to user about appeal approval
                notificationService.createNotification(user, 
                    "Your appeal has been approved! Your ban has been lifted and you can post again.", 
                    NotificationType.APPEAL_APPROVED);
                log.info("Appeal approval notification sent to user: {}", userId);
                
                // Verify the changes persisted
                log.debug("User {} status after save: {}", userId, savedUser.getStatus());
            } else if (appeal.getAppealType() == AppealType.POST) {
                // Restore the post - set status back to PUBLISHED
                Post post = appeal.getPost();
                post.setStatus(PostStatus.PUBLISHED);
                postRepository.save(post);
                log.info("Post restored: {} to PUBLISHED status", post.getId());
                
                // Send notification to post author
                User author = post.getAuthor();
                notificationService.createNotification(author,
                    "Your appeal has been approved! Your post has been restored.",
                    NotificationType.POST_APPROVED);
                log.info("Appeal approval notification sent to post author: {}", author.getId());
            }
        } else {
            log.info("Appeal REJECTED: {}", appealId);
            appeal.setStatus(AppealStatus.REJECTED);
            
            // Send rejection notification to user
            if (appeal.getAppealType() == AppealType.BAN) {
                User user = appeal.getBan().getUser();
                notificationService.createNotification(user,
                    "Your ban appeal has been rejected. " + decision,
                    NotificationType.APPEAL_REJECTED);
                log.info("Appeal rejection notification sent to user: {}", user.getId());
            } else if (appeal.getAppealType() == AppealType.POST) {
                User author = appeal.getPost().getAuthor();
                notificationService.createNotification(author,
                    "Your post appeal has been rejected. " + decision,
                    NotificationType.APPEAL_REJECTED);
                log.info("Appeal rejection notification sent to post author: {}", author.getId());
            }
            // Ban remains active or post remains removed
        }

        Appeal updatedAppeal = appealRepository.save(appeal);
        return mapToDTO(updatedAppeal);
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingAppealCount() {
        long count = appealRepository.countByStatus(AppealStatus.PENDING);
        log.debug("Pending appeals count: {}", count);
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public long getAppealCountByStatus(AppealStatus status) {
        long count = appealRepository.countByStatus(status);
        log.debug("Appeals count for status {}: {}", status, count);
        return count;
    }

    /**
     * Map Appeal entity to AppealDTO.
     */
    private AppealDTO mapToDTO(Appeal appeal) {
        String username = null;
        
        // Get username based on appeal type
        if (appeal.getAppealType() == AppealType.POST && appeal.getPost() != null) {
            username = appeal.getPost().getAuthor().getUsername();
        } else if (appeal.getAppealType() == AppealType.BAN && appeal.getBan() != null) {
            username = appeal.getBan().getUser().getUsername();
        }
        
        return AppealDTO.builder()
            .id(appeal.getId())
            .appealType(appeal.getAppealType())
            .banId(appeal.getBan() != null ? appeal.getBan().getId() : null)
            .postId(appeal.getPost() != null ? appeal.getPost().getId() : null)
            .username(username)
            .reason(appeal.getReason())
            .status(appeal.getStatus())
            .moderatorReview(appeal.getModeratorReview())
            .adminDecision(appeal.getAdminDecision())
            .createdAt(appeal.getCreatedAt())
            .updatedAt(appeal.getUpdatedAt())
            .build();
    }
}
