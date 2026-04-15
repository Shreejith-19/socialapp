package com.example.socialapp.frontend.util;

import com.example.socialapp.frontend.model.UserDTO;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for role-based access control.
 * Centralizes all role checks across the application.
 */
public class RoleManager {
    /**
     * Check if user has a specific role.
     */
    public static boolean hasRole(Optional<UserDTO> user, String role) {
        return user.map(u -> u.getRoles() != null && u.getRoles().contains(role))
                  .orElse(false);
    }

    /**
     * Check if user is an admin.
     */
    public static boolean isAdmin(Optional<UserDTO> user) {
        return hasRole(user, "ADMIN");
    }

    /**
     * Check if user is a moderator.
     */
    public static boolean isModerator(Optional<UserDTO> user) {
        return hasRole(user, "MODERATOR");
    }

    /**
     * Check if user is a regular user (no special roles).
     */
    public static boolean isRegularUser(Optional<UserDTO> user) {
        return user.isPresent() && !isAdmin(user) && !isModerator(user);
    }

    /**
     * Check if user is banned.
     */
    public static boolean isBanned(Optional<UserDTO> user) {
        return user.map(u -> u.getEnabled() == null || !u.getEnabled())
                   .orElse(false);
    }

    /**
     * Get highest privilege level.
     * Returns: ADMIN > MODERATOR > USER
     */
    public static String getHighestRole(Optional<UserDTO> user) {
        if (isAdmin(user)) return "ADMIN";
        if (isModerator(user)) return "MODERATOR";
        return "USER";
    }

    /**
     * Check if user can access admin panel.
     */
    public static boolean canAccessAdminPanel(Optional<UserDTO> user) {
        return isAdmin(user);
    }

    /**
     * Check if user can access moderation dashboard.
     */
    public static boolean canAccessModerationDashboard(Optional<UserDTO> user) {
        return isModerator(user) || isAdmin(user);
    }

    /**
     * Check if user can create posts.
     */
    public static boolean canCreatePosts(Optional<UserDTO> user) {
        return user.isPresent() && !isBanned(user);
    }

    /**
     * Get all roles as comma-separated string.
     */
    public static String getRolesAsString(Optional<UserDTO> user) {
        return user.map(u -> {
            Set<String> roles = u.getRoles();
            return roles != null && !roles.isEmpty() 
                   ? String.join(", ", roles)
                   : "USER";
        }).orElse("UNKNOWN");
    }
}
