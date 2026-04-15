package com.example.socialapp.controller;

import com.example.socialapp.dto.AppealDTO;
import com.example.socialapp.enums.AppealStatus;
import com.example.socialapp.enums.AppealType;
import com.example.socialapp.service.AppealService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Post Appeal Controller Endpoints.
 * Tests the HTTP API layer for post appeal functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Post Appeal Controller Integration Tests")
class AppealControllerPostAppealIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppealService appealService;

    private UUID postId;
    private UUID appealId;
    private AppealDTO postAppealDTO;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        appealId = UUID.randomUUID();

        // Create test appeal DTO
        postAppealDTO = AppealDTO.builder()
            .id(appealId)
            .appealType(AppealType.POST)
            .postId(postId)
            .banId(null)
            .reason("This post was removed incorrectly")
            .status(AppealStatus.PENDING)
            .moderatorReview(null)
            .adminDecision(null)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ============= POST /api/v1/appeals/posts Tests =============

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should submit post appeal successfully with valid request")
    void testSubmitPostAppealSuccess() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("This post was removed incorrectly");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenReturn(postAppealDTO);

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(appealId.toString()))
            .andExpect(jsonPath("$.appealType").value("POST"))
            .andExpect(jsonPath("$.postId").value(postId.toString()))
            .andExpect(jsonPath("$.banId").doesNotExist())
            .andExpect(jsonPath("$.reason").value("This post was removed incorrectly"))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 400 when postId is missing")
    void testSubmitPostAppealMissingPostId() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(null);
        request.setReason("Reason");

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Post ID is required")));

        verify(appealService, never()).submitPostAppeal(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 400 when reason is blank")
    void testSubmitPostAppealBlankReason() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("");

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Reason is required")));

        verify(appealService, never()).submitPostAppeal(any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 404 when post not found")
    void testSubmitPostAppealPostNotFound() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("This post was removed incorrectly");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenThrow(new com.example.socialapp.exception.ResourceNotFoundException("Post", "id", postId.toString()));

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 409 when post is not in REMOVED status")
    void testSubmitPostAppealPostNotRemoved() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenThrow(new IllegalStateException("Only posts with REMOVED status can be appealed"));

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 403 when user is not the post author")
    void testSubmitPostAppealNotAuthor() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenThrow(new com.example.socialapp.exception.ConflictException("You can only appeal posts you authored"));

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return 409 when appeal already exists for post")
    void testSubmitPostAppealDuplicate() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenThrow(new com.example.socialapp.exception.ConflictException("An appeal for this post already exists"));

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void testSubmitPostAppealNotAuthenticated() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());

        verify(appealService, never()).submitPostAppeal(any(), any());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    @DisplayName("Should allow MODERATOR role to submit post appeal")
    void testSubmitPostAppealModeratorRole() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenReturn(postAppealDTO);

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(appealId.toString()));

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should allow ADMIN role to submit post appeal")
    void testSubmitPostAppealAdminRole() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenReturn(postAppealDTO);

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(appealId.toString()));

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return content type application/json in response")
    void testSubmitPostAppealResponseContentType() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("Appeal reason");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenReturn(postAppealDTO);

        // Act & Assert
        mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(appealService).submitPostAppeal(eq(postId), any(String.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Verify response structure for post appeal creation")
    void testSubmitPostAppealResponseStructure() throws Exception {
        // Arrange
        AppealController.PostAppealRequest request = new AppealController.PostAppealRequest();
        request.setPostId(postId);
        request.setReason("This post was incorrectly flagged and removed");

        when(appealService.submitPostAppeal(eq(postId), any(String.class)))
            .thenReturn(postAppealDTO);

        // Act
        MvcResult result = mockMvc.perform(post("/v1/appeals/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        // Assert response contains all required fields
        String responseJson = result.getResponse().getContentAsString();
        AppealDTO responseDTO = objectMapper.readValue(responseJson, AppealDTO.class);

        assertEquals(appealId, responseDTO.getId());
        assertEquals(AppealType.POST, responseDTO.getAppealType());
        assertEquals(postId, responseDTO.getPostId());
        assertEquals(AppealStatus.PENDING, responseDTO.getStatus());
        assertEquals("This post was removed incorrectly", responseDTO.getReason());
    }
}
