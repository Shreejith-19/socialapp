package com.example.socialapp.frontend.util;

import com.example.socialapp.frontend.model.UserDTO;
import javafx.scene.control.Control;
import javafx.scene.layout.Region;
import java.util.Optional;

/**
 * Utility class for role-based UI element rendering.
 * Provides methods to show/hide UI elements based on user roles.
 */
public class RoleBasedUIRenderer {
    /**
     * Hide UI element if condition is false, showing it if true.
     */
    public static void setVisible(Control control, boolean condition) {
        control.setVisible(condition);
        control.setManaged(condition);
    }

    /**
     * Hide UI element if condition is false, showing it if true (for Region/VBox/HBox).
     */
    public static void setVisible(Region region, boolean condition) {
        region.setVisible(condition);
        region.setManaged(condition);
    }

    /**
     * Show UI element only if user has specific role.
     */
    public static void showIfRole(Control control, Optional<UserDTO> user, String role) {
        setVisible(control, RoleManager.hasRole(user, role));
    }

    /**
     * Show UI element only if user is admin.
     */
    public static void showIfAdmin(Control control, Optional<UserDTO> user) {
        setVisible(control, RoleManager.isAdmin(user));
    }

    /**
     * Show UI element only if user is moderator.
     */
    public static void showIfModerator(Control control, Optional<UserDTO> user) {
        setVisible(control, RoleManager.isModerator(user));
    }

    /**
     * Show UI element only if user is moderator or admin.
     */
    public static void showIfModeratorOrAdmin(Control control, Optional<UserDTO> user) {
        setVisible(control, RoleManager.isModerator(user) || RoleManager.isAdmin(user));
    }

    /**
     * Show UI element only if user is banned.
     */
    public static void showIfBanned(Control control, Optional<UserDTO> user) {
        setVisible(control, RoleManager.isBanned(user));
    }

    /**
     * Show UI element only if user can create posts.
     */
    public static void showIfCanCreatePosts(Control control, Optional<UserDTO> user) {
        setVisible(control, RoleManager.canCreatePosts(user));
    }

    /**
     * Disable UI element based on condition.
     */
    public static void setDisabled(Control control, boolean condition) {
        control.setDisable(condition);
    }

    /**
     * Disable UI element if user doesn't have specific role.
     */
    public static void disableIfNoRole(Control control, Optional<UserDTO> user, String role) {
        setDisabled(control, !RoleManager.hasRole(user, role));
    }

    /**
     * Disable UI element if user is not admin.
     */
    public static void disableIfNotAdmin(Control control, Optional<UserDTO> user) {
        setDisabled(control, !RoleManager.isAdmin(user));
    }

    /**
     * Disable UI element if user is not moderator.
     */
    public static void disableIfNotModerator(Control control, Optional<UserDTO> user) {
        setDisabled(control, !RoleManager.isModerator(user));
    }
}
