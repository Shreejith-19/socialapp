package com.example.socialapp.service.impl;

import com.example.socialapp.dto.AppealDTO;
import com.example.socialapp.entity.Appeal;
import com.example.socialapp.entity.Ban;
import com.example.socialapp.entity.Post;
import com.example.socialapp.entity.User;
import com.example.socialapp.enums.AppealStatus;
import com.example.socialapp.enums.AppealType;
import com.example.socialapp.enums.BanType;
import com.example.socialapp.enums.PostStatus;
import com.example.socialapp.enums.UserStatus;
import com.example.socialapp.exception.ConflictException;
import com.example.socialapp.exception.ResourceNotFoundException;
import com.example.socialapp.repository.AppealRepository;
import com.example.socialapp.repository.BanRepository;
import com.example.socialapp.repository.PostRepository;
import com.example.socialapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Post Appeal Functionality.
 * Tests the submission, validation, and workflow of post appeals.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Post Appeal Service Tests")
class AppealServiceImplPostAppealTest {

    @Mock
    private AppealRepository appealRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private BanRepository banRepository;

    @InjectMocks
    private AppealServiceImpl appealService;

    private UUID postId;
    private UUID userId;
    private UUID appealId;
    private User testUser;
    private Post removedPost;
    private Appeal postAppeal;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        userId = UUID.randomUUID();
        appealId = UUID.randomUUID();

        // Create test user
        testUser = User.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .status(UserStatus.ACTIVE)
            .build();

        // Create removed post
        removedPost = Post.builder()
            .id(postId)
            .content("Test post content")
            .status(PostStatus.REMOVED)
            .author(testUser)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Create post appeal
        postAppeal = Appeal.builder()
            .id(appealId)
            .appealType(AppealType.POST)
            .post(removedPost)
            .reason("This post was removed incorrectly")
            .status(AppealStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Setup security context
        setupSecurityContext("testuser");
    }

    private void setupSecurityContext(String username) {
        SecurityContext securityContext = mock(SecurityContext.class);
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(username, null);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    // ============= submitPostAppeal Tests =============

    @Test
    @DisplayName("Should submit post appeal successfully when post is removed and user is author")
    void testSubmitPostAppealSuccess() {
        // Arrange
        when(postRepository.findById(postId)).thenReturn(Optional.of(removedPost));
        when(appealRepository.existsByPostId(postId)).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(appealRepository.save(any(Appeal.class))).thenReturn(postAppeal);

        // Act
        AppealDTO result = appealService.submitPostAppeal(postId, "This post was removed incorrectly");

        // Assert
        assertNotNull(result);
        assertEquals(appealId, result.getId());
        assertEquals(AppealType.POST, result.getAppealType());
        assertEquals(postId, result.getPostId());
        assertNull(result.getBanId());
        assertEquals(AppealStatus.PENDING, result.getStatus());
        assertEquals("This post was removed incorrectly", result.getReason());

        verify(postRepository).findById(postId);
        verify(appealRepository).existsByPostId(postId);
        verify(userRepository).findByUsername("testuser");
        verify(appealRepository).save(any(Appeal.class));
    }

    @Test
    @DisplayName("Should throw exception when post not found")
    void testSubmitPostAppealPostNotFound() {
        // Arrange
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> appealService.submitPostAppeal(postId, "This post was removed incorrectly")
        );

        verify(postRepository).findById(postId);
        verify(appealRepository, never()).save(any(Appeal.class));
    }

    @Test
    @DisplayName("Should throw exception when post is not in REMOVED status")
    void testSubmitPostAppealPostNotRemoved() {
        // Arrange
        Post publishedPost = Post.builder()
            .id(postId)
            .content("Test content")
            .status(PostStatus.PUBLISHED)
            .author(testUser)
            .build();
        when(postRepository.findById(postId)).thenReturn(Optional.of(publishedPost));

        // Act & Assert
        assertThrows(
            IllegalStateException.class,
            () -> appealService.submitPostAppeal(postId, "Appeal reason"),
            "Only posts with REMOVED status can be appealed"
        );

        verify(appealRepository, never()).save(any(Appeal.class));
    }

    @Test
    @DisplayName("Should throw exception when appeal already exists for post")
    void testSubmitPostAppealDuplicateAppeal() {
        // Arrange
        when(postRepository.findById(postId)).thenReturn(Optional.of(removedPost));
        when(appealRepository.existsByPostId(postId)).thenReturn(true);

        // Act & Assert
        assertThrows(
            ConflictException.class,
            () -> appealService.submitPostAppeal(postId, "Appeal reason"),
            "An appeal for this post already exists"
        );

        verify(appealRepository, never()).save(any(Appeal.class));
    }

    @Test
    @DisplayName("Should throw exception when user is not the post author")
    void testSubmitPostAppealNotAuthor() {
        // Arrange
        User otherUser = User.builder()
            .id(UUID.randomUUID())
            .username("otheruser")
            .email("other@example.com")
            .status(UserStatus.ACTIVE)
            .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(removedPost));
        when(appealRepository.existsByPostId(postId)).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(otherUser));

        // Act & Assert
        assertThrows(
            ConflictException.class,
            () -> appealService.submitPostAppeal(postId, "Appeal reason"),
            "You can only appeal posts you authored"
        );

        verify(appealRepository, never()).save(any(Appeal.class));
    }

    @Test
    @DisplayName("Should throw exception when current user not found")
    void testSubmitPostAppealCurrentUserNotFound() {
        // Arrange
        when(postRepository.findById(postId)).thenReturn(Optional.of(removedPost));
        when(appealRepository.existsByPostId(postId)).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            ResourceNotFoundException.class,
            () -> appealService.submitPostAppeal(postId, "Appeal reason")
        );

        verify(appealRepository, never()).save(any(Appeal.class));
    }

    // ============= getAppealByPostId Tests =============

    @Test
    @DisplayName("Should retrieve appeal by post ID successfully")
    void testGetAppealByPostIdSuccess() {
        // Arrange
        when(appealRepository.findByPostId(postId)).thenReturn(Optional.of(postAppeal));

        // Act
        Optional<AppealDTO> result = appealService.getAppealByPostId(postId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(appealId, result.get().getId());
        assertEquals(AppealType.POST, result.get().getAppealType());
        assertEquals(postId, result.get().getPostId());

        verify(appealRepository).findByPostId(postId);
    }

    @Test
    @DisplayName("Should return empty when appeal not found for post")
    void testGetAppealByPostIdNotFound() {
        // Arrange
        when(appealRepository.findByPostId(postId)).thenReturn(Optional.empty());

        // Act
        Optional<AppealDTO> result = appealService.getAppealByPostId(postId);

        // Assert
        assertTrue(result.isEmpty());

        verify(appealRepository).findByPostId(postId);
    }

    // ============= adminDecision Tests for Post Appeal =============

    @Test
    @DisplayName("Should approve post appeal and restore post to PUBLISHED")
    void testAdminDecisionApprovePostAppeal() {
        // Arrange
        when(appealRepository.findById(appealId)).thenReturn(Optional.of(postAppeal));
        when(appealRepository.save(any(Appeal.class))).thenReturn(postAppeal);

        // Act
        AppealDTO result = appealService.adminDecision(
            appealId, 
            true, 
            "Appeal approved. Post restored."
        );

        // Assert
        assertNotNull(result);
        assertEquals(AppealStatus.APPROVED, result.getStatus());

        verify(appealRepository).findById(appealId);
        verify(postRepository).save(removedPost);
        verify(appealRepository).save(postAppeal);

        // Verify post status was changed to PUBLISHED
        assertEquals(PostStatus.PUBLISHED, removedPost.getStatus());
    }

    @Test
    @DisplayName("Should reject post appeal and keep post in REMOVED status")
    void testAdminDecisionRejectPostAppeal() {
        // Arrange
        when(appealRepository.findById(appealId)).thenReturn(Optional.of(postAppeal));
        when(appealRepository.save(any(Appeal.class))).thenReturn(postAppeal);

        // Act
        AppealDTO result = appealService.adminDecision(
            appealId, 
            false, 
            "Appeal rejected. Post removal upheld."
        );

        // Assert
        assertNotNull(result);
        assertEquals(AppealStatus.REJECTED, result.getStatus());

        verify(appealRepository).findById(appealId);
        verify(appealRepository).save(postAppeal);

        // Verify post status remains REMOVED (should not call postRepository.save)
        verify(postRepository, never()).save(any(Post.class));
        assertEquals(PostStatus.REMOVED, removedPost.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when approving non-pending post appeal")
    void testAdminDecisionNonPendingPostAppeal() {
        // Arrange
        Appeal approvedAppeal = Appeal.builder()
            .id(appealId)
            .appealType(AppealType.POST)
            .post(removedPost)
            .reason("Appeal reason")
            .status(AppealStatus.APPROVED)
            .build();

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(approvedAppeal));

        // Act & Assert
        assertThrows(
            IllegalStateException.class,
            () -> appealService.adminDecision(appealId, true, "Decision"),
            "Appeal is not pending decision"
        );

        verify(appealRepository, never()).save(any(Appeal.class));
    }

    // ============= DTO Mapping Tests =============

    @Test
    @DisplayName("Should correctly map appeal with post to DTO")
    void testMapPostAppealToDTO() {
        // Arrange
        when(appealRepository.findById(appealId)).thenReturn(Optional.of(postAppeal));

        // Act
        AppealDTO result = appealService.getAppealById(appealId);

        // Assert
        assertNotNull(result);
        assertEquals(appealId, result.getId());
        assertEquals(AppealType.POST, result.getAppealType());
        assertEquals(postId, result.getPostId());
        assertNull(result.getBanId());
        assertEquals("This post was removed incorrectly", result.getReason());
        assertEquals(AppealStatus.PENDING, result.getStatus());

        verify(appealRepository).findById(appealId);
    }

    @Test
    @DisplayName("Should correctly map appeal with ban to DTO")
    void testMapBanAppealToDTO() {
        // Arrange
        Ban ban = Ban.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .type(BanType.PERMANENT)
            .startDate(LocalDateTime.now())
            .reason("Policy violation")
            .build();

        Appeal banAppeal = Appeal.builder()
            .id(appealId)
            .appealType(AppealType.BAN)
            .ban(ban)
            .reason("Appeal against ban")
            .status(AppealStatus.PENDING)
            .build();

        when(appealRepository.findById(appealId)).thenReturn(Optional.of(banAppeal));

        // Act
        AppealDTO result = appealService.getAppealById(appealId);

        // Assert
        assertNotNull(result);
        assertEquals(appealId, result.getId());
        assertEquals(AppealType.BAN, result.getAppealType());
        assertEquals(ban.getId(), result.getBanId());
        assertNull(result.getPostId());
        assertEquals("Appeal against ban", result.getReason());

        verify(appealRepository).findById(appealId);
    }
}
