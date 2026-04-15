package com.example.socialapp.frontend.util;

import com.example.socialapp.frontend.model.UserDTO;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

/**
 * Session Manager for handling user session state.
 * Singleton pattern to maintain session across the application.
 */
@Slf4j
public class SessionManager {
    private static SessionManager instance;
    private UserDTO currentUser;
    private String authToken;

    private SessionManager() {
    }

    /**
     * Get singleton instance.
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Set current user and token.
     */
    public void setSession(UserDTO user, String token) {
        this.currentUser = user;
        this.authToken = token;
        log.info("Session created for user: {}", user.getEmail());
    }

    /**
     * Get current user.
     */
    public Optional<UserDTO> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Get auth token.
     */
    public Optional<String> getAuthToken() {
        return Optional.ofNullable(authToken);
    }

    /**
     * Clear session.
     */
    public void clearSession() {
        log.info("Session cleared");
        this.currentUser = null;
        this.authToken = null;
    }

    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return currentUser != null && authToken != null;
    }

    /**
     * Check if current user is an admin.
     */
    public boolean isAdmin() {
        return RoleManager.isAdmin(getCurrentUser());
    }

    /**
     * Check if current user is a moderator.
     */
    public boolean isModerator() {
        return RoleManager.isModerator(getCurrentUser());
    }

    /**
     * Check if current user is banned.
     */
    public boolean isBanned() {
        return RoleManager.isBanned(getCurrentUser());
    }

    /**
     * Check if current user has a specific role.
     */
    public boolean hasRole(String role) {
        return RoleManager.hasRole(getCurrentUser(), role);
    }

    /**
     * Get highest privilege level of current user.
     * Returns: ADMIN > MODERATOR > USER
     */
    public String getHighestRole() {
        return RoleManager.getHighestRole(getCurrentUser());
    }

    /**
     * Get all roles of current user as string.
     */
    public String getRolesAsString() {
        return RoleManager.getRolesAsString(getCurrentUser());
    }
}
