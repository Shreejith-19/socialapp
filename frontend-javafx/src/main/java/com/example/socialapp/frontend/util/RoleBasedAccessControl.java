package com.example.socialapp.frontend.util;

import com.example.socialapp.frontend.model.UserDTO;
import java.util.Optional;

/**
 * Role-Based Access Control (RBAC) Configuration.
 * Defines the complete role hierarchy and access rules for the application.
 * 
 * Role Hierarchy:
 * ADMIN > MODERATOR > USER (regular user)
 * 
 * Role Permissions:
 * - ADMIN: Full access to all features
 *   - View all users
 *   - Ban/unban users
 *   - Review and decide appeals
 *   - Access moderation dashboard
 *   - Create posts and feed access
 * 
 * - MODERATOR: Content moderation
 *   - Review flagged posts in moderation queue
 *   - Approve, remove, or escalate posts
 *   - Cannot manage users or appeals
 *   - Can create posts and view feed
 * 
 * - USER (Regular): Standard features
 *   - Create and view posts
 *   - View feed
 *   - Submit appeals if banned
 *   - Cannot access moderation or admin features
 */
public class RoleBasedAccessControl {
    public static final String ADMIN = "ADMIN";
    public static final String MODERATOR = "MODERATOR";
    public static final String USER = "USER";

    /**
     * Feature access matrix.
     * Shows which roles can access which features.
     */
    public static class Features {
        // Feed & Post Management
        public static boolean canViewFeed(Optional<UserDTO> user) {
            return user.isPresent();
        }

        public static boolean canCreatePost(Optional<UserDTO> user) {
            return RoleManager.canCreatePosts(user);
        }

        // Moderation Features
        public static boolean canAccessModerationDashboard(Optional<UserDTO> user) {
            return RoleManager.isModerator(user) || RoleManager.isAdmin(user);
        }

        public static boolean canMakeDecision(Optional<UserDTO> user) {
            return RoleManager.isModerator(user) || RoleManager.isAdmin(user);
        }

        // Admin Features
        public static boolean canAccessAdminPanel(Optional<UserDTO> user) {
            return RoleManager.isAdmin(user);
        }

        public static boolean canManageUsers(Optional<UserDTO> user) {
            return RoleManager.isAdmin(user);
        }

        public static boolean canReviewAppeals(Optional<UserDTO> user) {
            return RoleManager.isAdmin(user);
        }

        public static boolean canBanUser(Optional<UserDTO> user) {
            return RoleManager.isAdmin(user);
        }

        public static boolean canUnbanUser(Optional<UserDTO> user) {
            return RoleManager.isAdmin(user);
        }

        // Appeal Features
        public static boolean canSubmitAppeal(Optional<UserDTO> user) {
            return RoleManager.isBanned(user);
        }

        public static boolean canViewOwnAppeals(Optional<UserDTO> user) {
            return user.isPresent();
        }
    }

    /**
     * UI Element Visibility Rules.
     * Determines which UI elements should be shown for each role.
     */
    public static class UIVisibility {
        // Dashboard Toolbar Buttons
        public static boolean showAppealButton(Optional<UserDTO> user) {
            return Features.canSubmitAppeal(user);
        }

        public static boolean showModerationButton(Optional<UserDTO> user) {
            return Features.canAccessModerationDashboard(user);
        }

        public static boolean showAdminButton(Optional<UserDTO> user) {
            return Features.canAccessAdminPanel(user);
        }

        // Post Creation
        public static boolean showCreatePostSection(Optional<UserDTO> user) {
            return Features.canCreatePost(user);
        }

        // Moderation Elements
        public static boolean showDecisionButtons(Optional<UserDTO> user) {
            return Features.canMakeDecision(user);
        }

        // Admin Elements
        public static boolean showUserManagementTab(Optional<UserDTO> user) {
            return Features.canManageUsers(user);
        }

        public static boolean showAppealReviewTab(Optional<UserDTO> user) {
            return Features.canReviewAppeals(user);
        }
    }

    /**
     * Page/Scene Access Control.
     * Determines if user can access specific pages.
     */
    public static class PageAccess {
        public static boolean canAccessFeed(Optional<UserDTO> user) {
            return user.isPresent();
        }

        public static boolean canAccessSignup(Optional<UserDTO> user) {
            return !user.isPresent(); // Not authenticated
        }

        public static boolean canAccessLogin(Optional<UserDTO> user) {
            return !user.isPresent(); // Not authenticated
        }

        public static boolean canAccessModerationDashboard(Optional<UserDTO> user) {
            return Features.canAccessModerationDashboard(user);
        }

        public static boolean canAccessAdminPanel(Optional<UserDTO> user) {
            return Features.canAccessAdminPanel(user);
        }

        public static boolean canAccessAppealScreen(Optional<UserDTO> user) {
            return Features.canSubmitAppeal(user);
        }
    }

    /**
     * Get human-readable role description.
     */
    public static String getDescription(String role) {
        return switch (role) {
            case ADMIN -> "Administrator - Full system access";
            case MODERATOR -> "Moderator - Content moderation privileges";
            case USER -> "Regular User - Standard access";
            default -> "Unknown Role";
        };
    }

    /**
     * Get role badge color for UI display.
     */
    public static String getRoleColor(String role) {
        return switch (role) {
            case ADMIN -> "#d9534f";      // Red
            case MODERATOR -> "#0275d8";  // Blue
            case USER -> "#5cb85c";       // Green
            default -> "#666666";         // Gray
        };
    }

    /**
     * Get role icon/emoji for UI display.
     */
    public static String getRoleIcon(String role) {
        return switch (role) {
            case ADMIN -> "👑";       // Crown
            case MODERATOR -> "🛡️";   // Shield
            case USER -> "👤";        // User
            default -> "❓";          // Unknown
        };
    }
}
