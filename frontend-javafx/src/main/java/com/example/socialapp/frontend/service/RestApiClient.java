package com.example.socialapp.frontend.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.example.socialapp.frontend.model.*;
import com.example.socialapp.frontend.util.ApiException;
import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * REST API Client for communicating with the backend.
 * Handles authentication, CRUD operations, and error handling.
 */
@Slf4j
public class RestApiClient {
    private static final String BASE_URL = "http://localhost:8080/api/v1";
    private static final String CONTENT_TYPE = "application/json";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(java.time.LocalDateTime.class, 
            (com.google.gson.JsonDeserializer<java.time.LocalDateTime>) (json, typeOfT, context) -> 
                java.time.LocalDateTime.parse(json.getAsString()))
        .create();

    private String authToken;

    /**
     * Authenticate user with email and password.
     *
     * @param email user email
     * @param password user password
     * @return Optional containing AuthResponse if successful
     */
    public Optional<AuthResponse> authenticate(String email, String password) {
        try {
            AuthRequest request = AuthRequest.builder()
                .email(email)
                .password(password)
                .build();

            URL url = URI.create(BASE_URL + "/auth/login").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setDoOutput(true);

            String jsonRequest = GSON.toJson(request);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                AuthResponse response = GSON.fromJson(readResponse(conn), AuthResponse.class);
                this.authToken = response.getToken();
                log.info("Authentication successful for user: {}. Token set to: {}", email, this.authToken != null ? "present" : "null");
                return Optional.of(response);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Authentication failed with code: {} (unauthorized/forbidden)", responseCode);
                this.authToken = null;
                return Optional.empty();
            } else {
                log.warn("Authentication failed with code: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error during authentication", e);
            return Optional.empty();
        }
    }

    /**
     * Register a new user.
     *
     * @param email user email
     * @param password user password
     * @param firstName first name
     * @param lastName last name
     * @return Optional containing UserDTO if successful
     */
    public Optional<UserDTO> signup(String email, String password, String firstName, String lastName) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("email", email);
            request.addProperty("password", password);
            request.addProperty("firstName", firstName);
            request.addProperty("lastName", lastName);

            URL url = URI.create(BASE_URL + "/auth/signup").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                UserDTO response = GSON.fromJson(readResponse(conn), UserDTO.class);
                log.info("User registered successfully: {}", email);
                return Optional.of(response);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Registration failed with code: {} (unauthorized/forbidden)", responseCode);
                return Optional.empty();
            } else {
                log.warn("Registration failed with code: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error during registration", e);
            return Optional.empty();
        }
    }

    /**
     * Set the authentication token manually.
     */
    public void setAuthToken(String token) {
        this.authToken = token;
        log.info("Token set via setAuthToken(). New token value: {}", token != null ? "present" : "null");
    }

    /**
     * Clear the authentication token.
     */
    public void clearAuthToken() {
        this.authToken = null;
        log.info("Token cleared via clearAuthToken()");
    }

    /**
     * Get current authentication token.
     */
    public Optional<String> getAuthToken() {
        return Optional.ofNullable(authToken);
    }

    /**
     * Get current user information.
     */
    public Optional<UserDTO> getCurrentUser() {
        log.info("Getting current user. Token set: {}", authToken != null);
        if (authToken == null) {
            log.warn("Cannot get current user - no auth token set");
            return Optional.empty();
        }
        return get("/users/me", UserDTO.class);
    }

    /**
     * Get user by ID.
     */
    public Optional<UserDTO> getUserById(String userId) {
        return get("/users/" + userId, UserDTO.class);
    }

    /**
     * Get all published posts.
     */
    public Optional<PagedResponse<?>> getAllPosts(int page, int size) {
        try {
            URL url = URI.create(BASE_URL + "/posts?page=" + page + "&size=" + size).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                PagedResponse<?> pagedResponse = GSON.fromJson(response, new com.google.gson.reflect.TypeToken<PagedResponse<PostDTO>>(){}.getType());
                log.info("Fetched posts. Content size: {}", pagedResponse.getContent() != null ? pagedResponse.getContent().size() : 0);
                return Optional.of(pagedResponse);
            } else {
                log.warn("Get posts failed with status code: {}", responseCode);
            }
        } catch (Exception e) {
            log.error("Error in get posts request", e);
        }
        return Optional.empty();
    }

    /**
     * Get authenticated user's posts (including flagged and removed).
     * 
     * @param page page number (0-indexed)
     * @param size page size
     * @return Optional containing PagedResponse with user's posts
     */
    public Optional<PagedResponse<?>> getMyPosts(int page, int size) {
        if (authToken == null) {
            log.warn("Cannot get my posts - no auth token set");
            return Optional.empty();
        }
        
        try {
            URL url = URI.create(BASE_URL + "/posts/my?page=" + page + "&size=" + size).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                PagedResponse<?> pagedResponse = GSON.fromJson(response, new com.google.gson.reflect.TypeToken<PagedResponse<PostDTO>>(){}.getType());
                log.info("Fetched user's posts. Content size: {}", pagedResponse.getContent() != null ? pagedResponse.getContent().size() : 0);
                return Optional.of(pagedResponse);
            } else {
                log.warn("Get my posts failed with status code: {}", responseCode);
            }
        } catch (Exception e) {
            log.error("Error in get my posts request", e);
        }
        return Optional.empty();
    }

    /**
     * Get posts by author.
     */
    public Optional<PagedResponse<?>> getPostsByAuthor(String authorId, int page, int size) {
        try {
            URL url = URI.create(BASE_URL + "/posts/author/" + authorId + "?page=" + page + "&size=" + size).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                PagedResponse<?> pagedResponse = GSON.fromJson(response, new com.google.gson.reflect.TypeToken<PagedResponse<PostDTO>>(){}.getType());
                return Optional.of(pagedResponse);
            }
        } catch (Exception e) {
            log.error("Error in get posts by author request", e);
        }
        return Optional.empty();
    }

    /**
     * Get post by ID.
     */
    public Optional<PostDTO> getPostById(String postId) {
        return get("/posts/" + postId, PostDTO.class);
    }

    /**
     * Create a new post.
     * 
     * @throws ApiException if user is banned (403) or unauthorized (401)
     */
    public Optional<PostDTO> createPost(String content) throws ApiException {
        if (authToken == null) return Optional.empty();

        try {
            JsonObject jsonRequest = new JsonObject();
            jsonRequest.addProperty("content", content);

            URL url = URI.create(BASE_URL + "/posts").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                PostDTO response = GSON.fromJson(readResponse(conn), PostDTO.class);
                log.info("Post created successfully: {}", response.getId());
                return Optional.of(response);
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("User is banned (403)");
                String errorResponse = readResponse(conn);
                this.authToken = null;
                
                // Parse ban details from error response
                try {
                    JsonElement element = com.google.gson.JsonParser.parseString(errorResponse);
                    JsonObject errorObj = element.getAsJsonObject();
                    
                    String banType = errorObj.has("banType") ? errorObj.get("banType").getAsString() : "TEMPORARY";
                    Long remainingDays = errorObj.has("remainingDays") ? errorObj.get("remainingDays").getAsLong() : 0L;
                    Long remainingHours = errorObj.has("remainingHours") ? errorObj.get("remainingHours").getAsLong() : 0L;
                    String banMessage = errorObj.has("banMessage") ? errorObj.get("banMessage").getAsString() : "You are banned";
                    String message = errorObj.has("message") ? errorObj.get("message").getAsString() : "You are banned and cannot create posts";
                    
                    log.info("Parsed ban details - Type: {}, Days: {}, Hours: {}, Message: {}", 
                        banType, remainingDays, remainingHours, banMessage);
                    
                    throw new ApiException(responseCode, message, banType, remainingDays, remainingHours, banMessage);
                } catch (JsonSyntaxException e) {
                    log.warn("Failed to parse ban details from error response", e);
                    throw new ApiException(responseCode, "You are banned and cannot create posts", "TEMPORARY", 0L, 0L, "You are banned");
                }
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.warn("Unauthorized to create post (401)");
                this.authToken = null;
                throw new ApiException(responseCode, "Your session has expired");
            } else {
                log.warn("Failed to create post with code: {}", responseCode);
                return Optional.empty();
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating post", e);
            return Optional.empty();
        }
    }

    /**
     * Like a post.
     */
    public Optional<LikeDTO> likePost(String postId) {
        if (authToken == null) return Optional.empty();
        return post("/posts/" + postId + "/like", null, LikeDTO.class);
    }

    /**
     * Dislike a post.
     */
    public Optional<LikeDTO> dislikePost(String postId) {
        if (authToken == null) return Optional.empty();
        return post("/posts/" + postId + "/dislike", null, LikeDTO.class);
    }

    /**
     * Get like count for a post.
     */
    public long getLikeCount(String postId) {
        try {
            URL url = URI.create(BASE_URL + "/posts/" + postId + "/like-count").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return Long.parseLong(readResponse(conn).trim());
            }
        } catch (Exception e) {
            log.error("Error getting like count", e);
        }
        return 0;
    }

    /**
     * Generic GET request.
     */
    private <T> Optional<T> get(String endpoint, Class<T> responseType) {
        try {
            URL url = URI.create(BASE_URL + endpoint).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            int responseCode = conn.getResponseCode();
            log.info("GET {} - Response Code: {}", endpoint, responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                T response = GSON.fromJson(readResponse(conn), responseType);
                return Optional.of(response);
            } else {
                log.warn("GET {} failed with status code: {}", endpoint, responseCode);
            }
        } catch (Exception e) {
            log.error("Error in GET request: {}", endpoint, e);
        }
        return Optional.empty();
    }

    /**
     * Generic POST request.
     */
    private <T> Optional<T> post(String endpoint, Object body, Class<T> responseType) {
        try {
            URL url = URI.create(BASE_URL + endpoint).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            if (authToken != null) {
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
            }
            conn.setDoOutput(true);

            if (body != null) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(GSON.toJson(body).getBytes());
                }
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                T response = GSON.fromJson(readResponse(conn), responseType);
                return Optional.of(response);
            }
        } catch (Exception e) {
            log.error("Error in POST request: {}", endpoint, e);
        }
        return Optional.empty();
    }

    /**
     * Read response from HTTP connection.
     */
    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getResponseCode() >= 400 ? 
            conn.getErrorStream() : conn.getInputStream();

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Get moderation queue for moderators.
     */
    public Optional<PagedResponse<?>> getModerationQueue(int page, int size) {
        if (authToken == null) return Optional.empty();
        try {
            URL url = URI.create(BASE_URL + "/moderation/queue?page=" + page + "&size=" + size).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                PagedResponse<?> pagedResponse = GSON.fromJson(response, new com.google.gson.reflect.TypeToken<PagedResponse<PostDTO>>(){}.getType());
                log.info("Fetched moderation queue. Content size: {}", pagedResponse.getContent() != null ? pagedResponse.getContent().size() : 0);
                return Optional.of(pagedResponse);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Unauthorized access to moderation queue");
                this.authToken = null;
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting moderation queue", e);
        }
        return Optional.empty();
    }

    /**
     * Submit a moderation decision on a post.
     * 
     * @param postId the post ID
     * @param decisionType APPROVED, REMOVED, or ESCALATED
     * @param reason reason for the decision
     * @return Optional containing ModerationDecisionDTO if successful
     * @throws ApiException if unauthorized (401) or forbidden (403)
     */
    public Optional<ModerationDecisionDTO> submitDecision(String postId, String decisionType, String reason) throws ApiException {
        if (authToken == null) return Optional.empty();
        try {
            JsonObject request = new JsonObject();
            request.addProperty("postId", postId);
            request.addProperty("decisionType", decisionType);
            request.addProperty("reason", reason);

            URL url = URI.create(BASE_URL + "/moderation/decision").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                ModerationDecisionDTO response = GSON.fromJson(readResponse(conn), ModerationDecisionDTO.class);
                log.info("Decision submitted for post: {}", postId);
                return Optional.of(response);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.warn("Unauthorized to submit moderation decision (401)");
                this.authToken = null;
                throw new ApiException(responseCode, "Your session has expired");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Forbidden to submit moderation decision (403)");
                this.authToken = null;
                throw new ApiException(responseCode, "You do not have permission to submit moderation decisions");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error submitting moderation decision", e);
        }
        return Optional.empty();
    }

    /**
     * Submit an appeal for a ban or moderation decision.
     * 
     * @param banId the ban ID
     * @param reason reason for appeal
     * @return Optional containing AppealDTO if successful
     */
    public Optional<AppealDTO> submitAppeal(String banId, String reason) {
        if (authToken == null) return Optional.empty();
        try {
            JsonObject request = new JsonObject();
            request.addProperty("banId", banId);
            request.addProperty("reason", reason);

            URL url = URI.create(BASE_URL + "/appeals").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                AppealDTO response = GSON.fromJson(readResponse(conn), AppealDTO.class);
                log.info("Appeal submitted for ban: {}", banId);
                return Optional.of(response);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Unauthorized to submit appeal");
                this.authToken = null;
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error submitting appeal", e);
        }
        return Optional.empty();
    }

    /**
     * Submit an appeal for a removed post.
     * 
     * @param postId the post ID
     * @param reason reason for appeal
     * @return Optional containing response if successful
     */
    public Optional<Object> submitPostAppeal(String postId, String reason) {
        if (authToken == null) return Optional.empty();
        try {
            JsonObject request = new JsonObject();
            request.addProperty("postId", postId);
            request.addProperty("reason", reason);

            URL url = URI.create(BASE_URL + "/appeals/posts").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            log.info("Post appeal submission response code: {}", responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                String responseBody = readResponse(conn);
                log.info("Post appeal submitted successfully for post: {}", postId);
                return Optional.of(responseBody);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Unauthorized to submit post appeal (code: {})", responseCode);
                this.authToken = null;
                return Optional.empty();
            } else {
                String errorResponse = readResponse(conn);
                log.error("Post appeal submission failed. Code: {}, Response: {}", responseCode, errorResponse);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error submitting post appeal", e);
        }
        return Optional.empty();
    }

    /**
     * Get all users (admin only).
     */
    public Optional<PagedResponse<?>> getUsers(int page, int size) {
        if (authToken == null) return Optional.empty();
        try {
            URL url = URI.create(BASE_URL + "/admin/users?page=" + page + "&size=" + size).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                PagedResponse<?> pagedResponse = GSON.fromJson(response, new com.google.gson.reflect.TypeToken<PagedResponse<UserDTO>>(){}.getType());
                return Optional.of(pagedResponse);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Unauthorized access to users list");
                this.authToken = null;
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting users list", e);
        }
        return Optional.empty();
    }

    /**
     * Ban a user (admin only).
     * 
     * @param userId the user ID
     * @param reason reason for banning
     * @return true if successful, false otherwise
     * @throws ApiException if unauthorized (401) or forbidden (403)
     */
    public boolean banUser(String userId, String reason) throws ApiException {
        if (authToken == null) return false;
        try {
            JsonObject request = new JsonObject();
            request.addProperty("reason", reason);

            URL url = URI.create(BASE_URL + "/admin/users/" + userId + "/ban").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                log.info("User {} banned successfully", userId);
                return true;
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.warn("Unauthorized to ban user (401)");
                this.authToken = null;
                throw new ApiException(responseCode, "Your session has expired");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Forbidden to ban user (403)");
                this.authToken = null;
                throw new ApiException(responseCode, "You do not have permission to ban users");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error banning user", e);
        }
        return false;
    }

    /**
     * Unban a user (admin only).
     * 
     * @param userId the user ID
     * @return true if successful, false otherwise
     * @throws ApiException if unauthorized (401) or forbidden (403)
     */
    public boolean unbanUser(String userId) throws ApiException {
        if (authToken == null) return false;
        try {
            URL url = URI.create(BASE_URL + "/admin/users/" + userId + "/unban").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                log.info("User {} unbanned successfully", userId);
                return true;
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.warn("Unauthorized to unban user (401)");
                this.authToken = null;
                throw new ApiException(responseCode, "Your session has expired");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Forbidden to unban user (403)");
                this.authToken = null;
                throw new ApiException(responseCode, "You do not have permission to unban users");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error unbanning user", e);
        }
        return false;
    }

    /**
     * Get all appeals (admin only).
     */
    public Optional<PagedResponse<?>> getAppeals(int page, int size) {
        if (authToken == null) return Optional.empty();
        try {
            URL url = URI.create(BASE_URL + "/appeals/pending").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + authToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                @SuppressWarnings({"unchecked", "rawtypes"})
                List<com.example.socialapp.frontend.model.AppealDTO> appeals = GSON.fromJson(response, new com.google.gson.reflect.TypeToken<List<com.example.socialapp.frontend.model.AppealDTO>>(){}.getType());
                
                // Create a PagedResponse wrapper for the appeals list
                int totalSize = appeals.size();
                @SuppressWarnings({"unchecked", "rawtypes"})
                PagedResponse<?> pagedResponse = new PagedResponse(appeals, 1, totalSize, 0, size, totalSize, true, true, false);
                log.info("Fetched {} appeals from backend", appeals.size());
                return Optional.of(pagedResponse);
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || 
                       responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Unauthorized access to appeals list");
                this.authToken = null;
                return Optional.empty();
            } else {
                log.warn("Error fetching appeals. Response code: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error getting appeals list", e);
        }
        return Optional.empty();
    }

    /**
     * Make a final decision on an appeal (admin only).
     * 
     * @param appealId the appeal ID
     * @param decision APPROVED or REJECTED
     * @param adminDecision admin's decision and reasoning
     * @return true if successful, false otherwise
     * @throws ApiException if unauthorized (401) or forbidden (403)
     */
    public boolean decideAppeal(String appealId, String decision, String adminDecision) throws ApiException {
        if (authToken == null) return false;
        try {
            JsonObject request = new JsonObject();
            request.addProperty("approved", "APPROVED".equals(decision));
            request.addProperty("decision", adminDecision);

            URL url = URI.create(BASE_URL + "/appeals/" + appealId + "/decision").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(request.toString().getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                log.info("Appeal {} decided with: {}", appealId, decision);
                return true;
            } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                log.warn("Unauthorized to decide appeal (401)");
                this.authToken = null;
                throw new ApiException(responseCode, "Your session has expired");
            } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                log.warn("Forbidden to decide appeal (403)");
                this.authToken = null;
                throw new ApiException(responseCode, "You do not have permission to decide appeals");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deciding appeal", e);
        }
        return false;
    }

    /**
     * Paged API response wrapper.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PagedResponse<T> {
        private java.util.List<T> content;
        private int totalPages;
        private long totalElements;
        private int number;  // Current page number (0-indexed)
        private int size;    // Page size
        private int numberOfElements;
        private boolean first;
        private boolean last;
        private boolean empty;
    }
}
