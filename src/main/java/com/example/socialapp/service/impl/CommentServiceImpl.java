package com.example.socialapp.service.impl;

import com.example.socialapp.dto.CommentDTO;
import com.example.socialapp.entity.Comment;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.User;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.CommentRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import com.example.socialapp.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Comment Service Implementation.
 * Handles comment creation, editing, and deletion operations.
 */
@Slf4j
@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                            UserRepository userRepository,
                            PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Override
    public CommentDTO createComment(UUID authorId, UUID postId, String content) {
        log.info("Creating comment by user {} on post {}", authorId, postId);

        // Validate content
        if (content == null || content.trim().isEmpty()) {
            log.warn("Attempted to create comment with empty content");
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        // Validate user exists
        User author = userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId.toString()));

        // Validate post exists
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        // Create and save comment
        Comment comment = Comment.builder()
            .content(content)
            .author(author)
            .post(post)
            .build();

        Comment savedComment = commentRepository.save(comment);
        log.info("Comment created successfully: {}", savedComment.getId());

        return convertToDTO(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDTO getCommentById(UUID commentId) {
        log.debug("Fetching comment with ID: {}", commentId);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId.toString()));

        return convertToDTO(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsByPost(UUID postId, Pageable pageable) {
        log.debug("Fetching comments for post: {}", postId);

        // Verify post exists
        postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId.toString()));

        return commentRepository.findByPostId(postId, pageable)
            .map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDTO> getCommentsByAuthor(UUID authorId, Pageable pageable) {
        log.debug("Fetching comments by author: {}", authorId);

        // Verify user exists
        userRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", authorId.toString()));

        return commentRepository.findByAuthorId(authorId, pageable)
            .map(this::convertToDTO);
    }

    @Override
    public CommentDTO updateComment(UUID commentId, String newContent, UUID userId) {
        log.info("Updating comment {} by user {}", commentId, userId);

        // Validate content
        if (newContent == null || newContent.trim().isEmpty()) {
            log.warn("Attempted to update comment with empty content");
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        // Get comment
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId.toString()));

        // Verify user is the comment author
        if (!comment.getAuthor().getId().equals(userId)) {
            log.warn("User {} attempted to update comment {} owned by {}", userId, commentId, comment.getAuthor().getId());
            throw new IllegalArgumentException("You can only edit your own comments");
        }

        // Update comment
        comment.edit(newContent);
        Comment updatedComment = commentRepository.save(comment);
        log.info("Comment updated successfully: {}", updatedComment.getId());

        return convertToDTO(updatedComment);
    }

    @Override
    public void deleteComment(UUID commentId, UUID userId) {
        log.info("Deleting comment {} by user {}", commentId, userId);

        // Get comment
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId.toString()));

        // Verify user is the comment author
        if (!comment.getAuthor().getId().equals(userId)) {
            log.warn("User {} attempted to delete comment {} owned by {}", userId, commentId, comment.getAuthor().getId());
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        // Verify comment can be deleted
        if (!comment.canDelete()) {
            log.warn("Comment {} cannot be deleted", commentId);
            throw new IllegalArgumentException("This comment cannot be deleted");
        }

        commentRepository.delete(comment);
        log.info("Comment deleted successfully: {}", commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCommentCount(UUID postId) {
        log.debug("Fetching comment count for post: {}", postId);
        return commentRepository.countByPostId(postId);
    }

    /**
     * Convert Comment entity to CommentDTO.
     *
     * @param comment the Comment entity
     * @return the CommentDTO
     */
    private CommentDTO convertToDTO(Comment comment) {
        return CommentDTO.builder()
            .id(comment.getId())
            .content(comment.getContent())
            .authorId(comment.getAuthor().getId())
            .authorUsername(comment.getAuthor().getUsername())
            .postId(comment.getPost().getId())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}
