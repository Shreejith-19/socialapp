package com.example.socialapp.frontend.service;

import com.example.socialapp.frontend.model.NotificationDTO;
import com.example.socialapp.frontend.util.SessionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Notification Service.
 * Handles notification API calls to the backend.
 * Singleton pattern to ensure shared state across application.
 */
@Slf4j
public class NotificationService {
    
    private static NotificationService instance;
    private static final String API_BASE = "/v1/notifications";
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class,
            (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                LocalDateTime.parse(json.getAsString()))
        .create();
    
    private final SessionManager sessionManager;
    
    private NotificationService() {
        this.sessionManager = SessionManager.getInstance();
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    /**
     * Get all notifications for current user with pagination.
     * Sorted by latest (newest first).
     */
    public Optional<RestApiClient.PagedResponse<NotificationDTO>> getAllNotifications(int page, int size) {
        try {
            String token = sessionManager.getAuthToken().orElse(null);
            if (token == null) {
                log.warn("No auth token available");
                return Optional.empty();
            }
            
            String url = BASE_URL + API_BASE + "/page?page=" + page + "&size=" + size + "&sort=createdAt,desc";
            
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                JsonObject jsonObj = GSON.fromJson(response, JsonObject.class);
                
                JsonArray contentArray = jsonObj.getAsJsonArray("content");
                List<NotificationDTO> notifications = new ArrayList<>();
                
                for (var element : contentArray) {
                    NotificationDTO notification = GSON.fromJson(element, NotificationDTO.class);
                    notifications.add(notification);
                }
                
                int totalPages = jsonObj.get("totalPages").getAsInt();
                long totalElements = jsonObj.get("totalElements").getAsLong();
                int pageNumber = jsonObj.get("number").getAsInt();
                int pageSize = jsonObj.get("size").getAsInt();
                int numberOfElements = jsonObj.get("numberOfElements").getAsInt();
                boolean isFirst = jsonObj.get("first").getAsBoolean();
                boolean isLast = jsonObj.get("last").getAsBoolean();
                boolean isEmpty = jsonObj.get("empty").getAsBoolean();
                
                RestApiClient.PagedResponse<NotificationDTO> pagedResponse = 
                    new RestApiClient.PagedResponse<>(notifications, totalPages, totalElements, 
                        pageNumber, pageSize, numberOfElements, isFirst, isLast, isEmpty);
                
                log.info("Fetched {} notifications", notifications.size());
                return Optional.of(pagedResponse);
            } else {
                log.warn("Failed to fetch notifications. Status: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error fetching notifications", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get unread notifications for current user.
     */
    public Optional<List<NotificationDTO>> getUnreadNotifications() {
        try {
            String token = sessionManager.getAuthToken().orElse(null);
            if (token == null) {
                log.warn("No auth token available");
                return Optional.empty();
            }
            
            String url = BASE_URL + API_BASE + "/unread";
            
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                JsonArray jsonArray = GSON.fromJson(response, JsonArray.class);
                
                List<NotificationDTO> notifications = new ArrayList<>();
                for (var element : jsonArray) {
                    NotificationDTO notification = GSON.fromJson(element, NotificationDTO.class);
                    notifications.add(notification);
                }
                
                log.info("Fetched {} unread notifications", notifications.size());
                return Optional.of(notifications);
            } else {
                log.warn("Failed to fetch unread notifications. Status: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error fetching unread notifications", e);
            return Optional.empty();
        }
    }
    
    /**
     * Get unread notification count.
     */
    public Optional<Long> getUnreadCount() {
        try {
            String token = sessionManager.getAuthToken().orElse(null);
            if (token == null) {
                log.warn("No auth token available");
                return Optional.empty();
            }
            
            String url = BASE_URL + API_BASE + "/unread/count";
            
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readResponse(conn);
                JsonObject jsonObj = GSON.fromJson(response, JsonObject.class);
                long count = jsonObj.get("unreadCount").getAsLong();
                
                log.info("Unread notification count: {}", count);
                return Optional.of(count);
            } else {
                log.warn("Failed to fetch unread count. Status: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error fetching unread notification count", e);
            return Optional.empty();
        }
    }
    
    /**
     * Mark a single notification as read.
     */
    public Optional<NotificationDTO> markAsRead(UUID notificationId) {
        try {
            String token = sessionManager.getAuthToken().orElse(null);
            if (token == null) {
                log.warn("No auth token available");
                return Optional.empty();
            }
            
            String url = BASE_URL + API_BASE + "/" + notificationId + "/read";
            
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                String response = readResponse(conn);
                if (response.isEmpty()) {
                    return Optional.empty();
                }
                NotificationDTO notification = GSON.fromJson(response, NotificationDTO.class);
                log.info("Marked notification {} as read", notificationId);
                return Optional.of(notification);
            } else {
                log.warn("Failed to mark notification as read. Status: {}", responseCode);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            return Optional.empty();
        }
    }
    
    /**
     * Mark all notifications as read.
     */
    public boolean markAllAsRead() {
        try {
            String token = sessionManager.getAuthToken().orElse(null);
            if (token == null) {
                log.warn("No auth token available");
                return false;
            }
            
            String url = BASE_URL + API_BASE + "/read-all";
            
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                log.info("Marked all notifications as read");
                return true;
            } else {
                log.warn("Failed to mark all notifications as read. Status: {}", responseCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error marking all notifications as read", e);
            return false;
        }
    }
    
    /**
     * Delete a notification.
     */
    public boolean deleteNotification(UUID notificationId) {
        try {
            String token = sessionManager.getAuthToken().orElse(null);
            if (token == null) {
                log.warn("No auth token available");
                return false;
            }
            
            String url = BASE_URL + API_BASE + "/" + notificationId;
            
            URL urlObj = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                log.info("Deleted notification {}", notificationId);
                return true;
            } else {
                log.warn("Failed to delete notification. Status: {}", responseCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting notification", e);
            return false;
        }
    }
    
    /**
     * Read HTTP response body.
     */
    private String readResponse(HttpURLConnection conn) throws Exception {
        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
