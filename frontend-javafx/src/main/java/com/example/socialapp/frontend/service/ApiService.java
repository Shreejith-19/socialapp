package com.example.socialapp.frontend.service;

import com.example.socialapp.frontend.model.*;
import com.example.socialapp.frontend.util.ApiException;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

/**
 * API Service Layer.
 * Provides high-level API operations for the frontend.
 * 
 * This is a singleton to ensure all controllers share the same API client
 * and authentication state.
 */
@Slf4j
public class ApiService {
    private static ApiService instance;
    private final RestApiClient restClient;

    private ApiService() {
        this.restClient = new RestApiClient();
    }

    /**
     * Get singleton instance.
     */
    public static synchronized ApiService getInstance() {
        if (instance == null) {
            instance = new ApiService();
        }
        return instance;
    }

    /**
     * Authenticate user.
     */
    public Optional<AuthResponse> login(String email, String password) {
        log.info("Attempting to login user: {}", email);
        return restClient.authenticate(email, password);
    }

    /**
     * Register new user.
     */
    public Optional<UserDTO> signup(String email, String password, String firstName, String lastName) {
        log.info("Attempting to signup user: {}", email);
        return restClient.signup(email, password, firstName, lastName);
    }

    /**
     * Logout user.
     */
    public void logout() {
        log.info("User logout");
        restClient.setAuthToken(null);
    }

    /**
     * Get current user.
     */
    public Optional<UserDTO> getCurrentUser() {
        return restClient.getCurrentUser();
    }

    /**
     * Refresh current user session by fetching latest user data from backend.
     * Updates the SessionManager with the new user data.
     */
    public void refreshCurrentUser() {
        log.info("Refreshing current user session from backend");
        Optional<UserDTO> updatedUser = getCurrentUser();
        
        if (updatedUser.isPresent()) {
            // Get the current auth token
            Optional<String> token = com.example.socialapp.frontend.util.SessionManager.getInstance().getAuthToken();
            
            if (token.isPresent()) {
                // Update the session with the new user data
                com.example.socialapp.frontend.util.SessionManager.getInstance().setSession(updatedUser.get(), token.get());
                log.info("User session refreshed for: {}", updatedUser.get().getEmail());
            }
        } else {
            log.warn("Could not refresh user session - user not found");
        }
    }

    /**
     * Get feed (all posts, paginated).
     */
    public Optional<RestApiClient.PagedResponse<?>> getFeed(int page, int size) {
        log.info("Fetching feed: page={}, size={}", page, size);
        return restClient.getAllPosts(page, size);
    }

    /**
     * Get posts (paginated).
     */
    public Optional<RestApiClient.PagedResponse<?>> getPosts(int page, int size) {
        return restClient.getAllPosts(page, size);
    }

    /**
     * Create a new post.
     * 
     * @throws ApiException if user is banned (403) or unauthorized (401)
     */
    public Optional<PostDTO> createPost(String content) throws ApiException {
        log.info("Creating new post");
        return restClient.createPost(content);
    }

    /**
     * Get moderation queue.
     */
    public Optional<RestApiClient.PagedResponse<?>> getModerationQueue(int page, int size) {
        log.info("Fetching moderation queue: page={}, size={}", page, size);
        return restClient.getModerationQueue(page, size);
    }

    /**
     * Submit a moderation decision.
     * 
     * @throws ApiException if unauthorized or forbidden
     */
    public Optional<ModerationDecisionDTO> submitDecision(String postId, String decisionType, String reason) throws ApiException {
        log.info("Submitting moderation decision for post: {} ({})", postId, decisionType);
        return restClient.submitDecision(postId, decisionType, reason);
    }

    /**
     * Submit an appeal.
     */
    public Optional<AppealDTO> submitAppeal(String banId, String reason) {
        log.info("Submitting appeal for ban: {}", banId);
        return restClient.submitAppeal(banId, reason);
    }

    /**
     * Submit a post appeal.
     */
    public Optional<Object> submitPostAppeal(String postId, String reason) {
        log.info("Submitting appeal for post: {}", postId);
        return restClient.submitPostAppeal(postId, reason);
    }

    /**
     * Get all users (admin only).
     */
    public Optional<RestApiClient.PagedResponse<?>> getUsers(int page, int size) {
        log.info("Fetching users list: page={}, size={}", page, size);
        return restClient.getUsers(page, size);
    }

    /**
     * Get authenticated user's posts (including flagged and removed).
     * 
     * @param page page number (0-indexed)
     * @param size page size
     * @return Optional containing PagedResponse with user's posts
     */
    public Optional<RestApiClient.PagedResponse<?>> getMyPosts(int page, int size) {
        log.info("Fetching user's posts: page={}, size={}", page, size);
        return restClient.getMyPosts(page, size);
    }

    /**
     * Ban a user (admin only).
     * 
     * @throws ApiException if unauthorized or forbidden
     */
    public boolean banUser(String userId, String reason) throws ApiException {
        log.info("Banning user: {}", userId);
        return restClient.banUser(userId, reason);
    }

    /**
     * Unban a user (admin only).
     * 
     * @throws ApiException if unauthorized or forbidden
     */
    public boolean unbanUser(String userId) throws ApiException {
        log.info("Unbanning user: {}", userId);
        return restClient.unbanUser(userId);
    }

    /**
     * Get all appeals (admin only).
     */
    public Optional<RestApiClient.PagedResponse<?>> getAppeals(int page, int size) {
        log.info("Fetching appeals list: page={}, size={}", page, size);
        return restClient.getAppeals(page, size);
    }

    /**
     * Make a final decision on an appeal (admin only).
     * 
     * @throws ApiException if unauthorized or forbidden
     */
    public boolean decideAppeal(String appealId, String decision, String adminDecision) throws ApiException {
        log.info("Making appeal decision: {} ({})", appealId, decision);
        return restClient.decideAppeal(appealId, decision, adminDecision);
    }

    /**
     * Like a post.
     */
    public Optional<LikeDTO> likePost(String postId) {
        return restClient.likePost(postId);
    }

    /**
     * Dislike a post.
     */
    public Optional<LikeDTO> dislikePost(String postId) {
        return restClient.dislikePost(postId);
    }

    /**
     * Get like count for a post.
     */
    public long getLikeCount(String postId) {
        return restClient.getLikeCount(postId);
    }

    /**
     * Check if user is authenticated.
     */
    public boolean isAuthenticated() {
        return restClient.getAuthToken().isPresent();
    }

    /**
     * Get REST client (for direct access if needed).
     */
    public RestApiClient getRestClient() {
        return restClient;
    }
}
