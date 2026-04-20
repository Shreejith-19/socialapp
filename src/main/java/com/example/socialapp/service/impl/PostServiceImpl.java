package com.example.socialapp.service.impl;

import com.example.socialapp.dto.PostDTO;
import com.example.socialapp.entity.Ban;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.NotificationType;
import com.example.socialapp.enums.PostStatus;
import com.example.socialapp.exception.BannedUserException;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.BanRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.ModerationService;
import com.example.socialapp.service.NotificationService;
import com.example.socialapp.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Post Service Implementation.
 * Handles post creation with automatic moderation workflow.
 */
@Slf4j
@Service
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ModerationService moderationService;
    private final BanRepository banRepository;
    private final NotificationService notificationService;

    public PostServiceImpl(PostRepository postRepository, 
                         UserRepository userRepository,
                         ModerationService moderationService,
                         BanRepository banRepository,
                         NotificationService notificationService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.moderationService = moderationService;
        this.banRepository = banRepository;
        this.notificationService = notificationService;
    }

    @Override
    public PostDTO createPost(UUID authorId, String content) {
        log.info("Creating new post for author: {}", authorId);

        // Step 1: Validate content
        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempted to create post with empty content");
            throw new IllegalArgumentException("Post content cannot be empty");
        }

        // Step 2: Get author and check if they can post
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId.toString()));

        if (!author.canPost()) {
            log.warn("User {} attempted to post while banned. Status: {}", authorId, author.getStatus());
            
            // Get ban details to include in exception
            java.util.Optional<Ban> activeBan = banRepository.findLatestActiveBanByUser(author);
            if (activeBan.isPresent()) {
                Ban ban = activeBan.get();
                long remainingDays = ban.getRemainingDays();
                long remainingHours = ban.getRemainingHours();
                String banMessage = ban.getFormattedRemainingTime();
                throw new BannedUserException(
                    "You cannot post due to your account status: " + author.getStatus(),
                    remainingDays,
                    remainingHours,
                    "You are temporarily banned. " + banMessage,
                    ban.isTemporary()
                );
            } else {
                throw new BannedUserException(
                    "Your account has been permanently banned.",
                    0,
                    0,
                    "Your account has been permanently banned.",
                    false
                );
            }
        }

        log.debug("User {} is eligible to post", authorId);

        // Step 3: Review content using moderation service
        boolean isContentSafe = moderationService.reviewContent(content);
        log.debug("Content moderation result for user {}: {}", authorId, isContentSafe ? "SAFE" : "FLAGGED");

        // Step 4: Create post and set status based on moderation result
        Post post = Post.builder()
            .content(content)
            .author(author)
            .status(isContentSafe ? PostStatus.PUBLISHED : PostStatus.FLAGGED)
            .build();

        Post savedPost = postRepository.save(post);
        log.info("Post created successfully: {} with status: {}", savedPost.getId(), savedPost.getStatus());

        // Send notifications based on post status
        if (isContentSafe) {
            // Notify user that post was published
            notificationService.createNotification(author,
                "Your post has been published and is now visible to other users.",
                NotificationType.POST_PUBLISHED);
            log.info("POST_PUBLISHED notification sent to author: {}", authorId);
        } else {
            // Notify user that post was flagged for review
            notificationService.createNotification(author,
                "Your post has been flagged for moderation review and is currently hidden.",
                NotificationType.POST_FLAGGED);
            log.info("POST_FLAGGED notification sent to author: {}", authorId);
        }

        // Step 5: If flagged, send to moderation queue
        if (!isContentSafe) {
            log.info("Flagging post {} for moderation review", savedPost.getId());
            moderationService.sendToQueue(savedPost);
        }

        return mapToDTO(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PostDTO getPostById(UUID postId) {
        log.info("Fetching post with ID: {}", postId);
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));
        return mapToDTO(post);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> getPostsByAuthor(UUID authorId, Pageable pageable) {
        log.info("Fetching posts by author: {}", authorId);
        
        // Verify author exists
        if (!userRepository.existsById(authorId)) {
            throw new ResourceNotFoundException("User", "id", authorId.toString());
        }

        return postRepository.findByAuthorId(authorId, pageable)
            .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> getMyPosts(UUID userId, Pageable pageable) {
        log.info("Fetching all posts for authenticated user: {}", userId);
        
        if (userId == null) {
            log.error("Attempted to fetch posts with null user ID");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Verify user exists - this is a critical security check
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("User not found for ID: {}", userId);
                return new ResourceNotFoundException("User", "id", userId.toString());
            });
        
        log.debug("Verified user exists: {} ({})", user.getEmail(), userId);

        // Return all posts created by the user regardless of status
        // (PUBLISHED, FLAGGED, REMOVED, etc.)
        // This query is restricted to this specific user ID - no cross-user data exposure
        Page<PostDTO> userPosts = postRepository.findByAuthorId(userId, pageable)
            .map(this::mapToDTO);
        
        log.debug("Found {} posts for user: {}", userPosts.getTotalElements(), userId);
        return userPosts;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostDTO> getAllPublishedPosts(Pageable pageable) {
        log.info("Fetching all published posts");
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable)
            .map(this::mapToDTO);
    }

    @Override
    public void deletePost(UUID postId) {
        log.info("Deleting post with ID: {}", postId);
        
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId.toString());
        }

        postRepository.deleteById(postId);
        log.info("Post deleted successfully: {}", postId);
    }

    /**
     * Map Post entity to PostDTO.
     *
     * @param post the post entity
     * @return the post DTO
     */
    private PostDTO mapToDTO(Post post) {
        return PostDTO.builder()
            .id(post.getId())
            .content(post.getContent())
            .status(post.getStatus())
            .authorId(post.getAuthor().getId())
            .authorUsername(post.getAuthor().getUsername())
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}
