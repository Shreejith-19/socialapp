package com.example.socialapp.service.impl;

import com.example.socialapp.dto.LikeDTO;
import com.example.socialapp.entity.Like;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.User;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.LikeRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.LikeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

/**
 * Like Service Implementation.
 * Handles like/dislike operations for posts.
 */
@Slf4j
@Service
@Transactional
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public LikeServiceImpl(LikeRepository likeRepository,
                         UserRepository userRepository,
                         PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Override
    public LikeDTO likePost(UUID userId, UUID postId) {
        log.info("User {} is liking post {}", userId, postId);

        // Validate user and post exist
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        // Check if like already exists
        Optional<Like> existingLike = likeRepository.findByUserIdAndPostId(userId, postId);

        Like like;
        if (existingLike.isPresent()) {
            // Update existing like to be a like
            like = existingLike.get();
            if (Boolean.TRUE.equals(like.getIsLike())) {
                log.debug("User {} already liked post {}", userId, postId);
            } else {
                log.debug("User {} is changing dislike to like for post {}", userId, postId);
                like.setIsLike(true);
            }
        } else {
            // Create new like
            like = Like.builder()
                .user(user)
                .post(post)
                .isLike(true)
                .build();
            log.debug("Creating new like for user {} on post {}", userId, postId);
        }

        Like savedLike = likeRepository.save(like);
        log.info("Like saved successfully: {}", savedLike.getId());

        return convertToDTO(savedLike);
    }

    @Override
    public LikeDTO dislikePost(UUID userId, UUID postId) {
        log.info("User {} is disliking post {}", userId, postId);

        // Validate user and post exist
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        // Check if like already exists
        Optional<Like> existingLike = likeRepository.findByUserIdAndPostId(userId, postId);

        Like like;
        if (existingLike.isPresent()) {
            // Update existing like to be a dislike
            like = existingLike.get();
            if (Boolean.FALSE.equals(like.getIsLike())) {
                log.debug("User {} already disliked post {}", userId, postId);
            } else {
                log.debug("User {} is changing like to dislike for post {}", userId, postId);
                like.setIsLike(false);
            }
        } else {
            // Create new dislike
            like = Like.builder()
                .user(user)
                .post(post)
                .isLike(false)
                .build();
            log.debug("Creating new dislike for user {} on post {}", userId, postId);
        }

        Like savedLike = likeRepository.save(like);
        log.info("Dislike saved successfully: {}", savedLike.getId());

        return convertToDTO(savedLike);
    }

    @Override
    public void removeLike(UUID userId, UUID postId) {
        log.info("Removing like/dislike for user {} on post {}", userId, postId);

        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
            .orElseThrow(() -> new ResourceNotFoundException("Like", "userId and postId", 
                userId + " and " + postId));

        likeRepository.delete(like);
        log.info("Like removed successfully for user {} on post {}", userId, postId);
    }

    @Override
    public long getLikeCount(UUID postId) {
        log.debug("Fetching like count for post {}", postId);
        return likeRepository.countByPostIdAndIsLikeTrue(postId);
    }

    @Override
    public long getDislikeCount(UUID postId) {
        log.debug("Fetching dislike count for post {}", postId);
        return likeRepository.countByPostIdAndIsLikeFalse(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserLiked(UUID userId, UUID postId) {
        return likeRepository.findByUserIdAndPostId(userId, postId)
            .map(like -> Boolean.TRUE.equals(like.getIsLike()))
            .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserDisliked(UUID userId, UUID postId) {
        return likeRepository.findByUserIdAndPostId(userId, postId)
            .map(like -> Boolean.FALSE.equals(like.getIsLike()))
            .orElse(false);
    }

    /**
     * Convert Like entity to LikeDTO.
     *
     * @param like the Like entity
     * @return the LikeDTO
     */
    private LikeDTO convertToDTO(Like like) {
        return LikeDTO.builder()
            .id(like.getId())
            .userId(like.getUser().getId())
            .userUsername(like.getUser().getUsername())
            .postId(like.getPost().getId())
            .isLike(like.getIsLike())
            .createdAt(like.getCreatedAt())
            .build();
    }
}
