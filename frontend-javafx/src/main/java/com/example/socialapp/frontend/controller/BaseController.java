package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.service.ApiService;
import com.example.socialapp.frontend.util.SessionManager;
import com.example.socialapp.frontend.util.AlertUtils;
import com.example.socialapp.frontend.util.ErrorHandler;
import com.example.socialapp.frontend.util.ApiException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

/**
 * Base Controller.
 * Provides common functionality for all controllers.
 * 
 * ROLE-BASED UI RENDERING:
 * ========================
 * All controllers use centralized role management for UI rendering.
 * 
 * Key Classes:
 * - RoleManager: Core role checking methods (isAdmin, isModerator, isBanned, etc.)
 * - RoleBasedUIRenderer: Utility methods to show/hide UI elements based on roles
 * - RoleBasedAccessControl: Complete RBAC configuration and feature matrix
 * - SessionManager: Convenience role check methods on current session
 * 
 * Usage Pattern:
 * 1. Check role using: RoleManager.isAdmin(sessionManager.getCurrentUser())
 * 2. Update UI using: RoleBasedUIRenderer.showIfAdmin(button, sessionManager.getCurrentUser())
 * 3. Query features using: RoleBasedAccessControl.Features.canAccessAdminPanel(user)
 * 
 * Example:
 *   private void setupAdminButton() {
 *       if (RoleManager.canAccessAdminPanel(sessionManager.getCurrentUser())) {
 *           adminButton.setOnAction(event -> navigateToAdmin());
 *       }
 *       RoleBasedUIRenderer.showIfAdmin(adminButton, sessionManager.getCurrentUser());
 *   }
 * 
 * Role Hierarchy: ADMIN > MODERATOR > USER
 * 
 * Navigation Flow by Role:
 * - All users land on dashboard.fxml after login
 * - Buttons are conditionally shown based on role:
 *   - Appeal Button: Shown only for banned users
 *   - Moderation Button: Shown for MODERATOR and ADMIN
 *   - Admin Button: Shown only for ADMIN
 * - Access control enforced in controller.initialize() before setup
 */
@Slf4j
public abstract class BaseController {
    protected ApiService apiService = ApiService.getInstance();
    protected SessionManager sessionManager = SessionManager.getInstance();

    /**
     * Navigate to a different view.
     */
    protected void navigateTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/socialapp/frontend/view/" + fxmlFile)
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            Stage stage = (Stage) ((Node) root).getScene().getWindow();
            if (stage == null) {
                stage = new Stage();
            }
            
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            log.error("Error navigating to {}", fxmlFile, e);
        }
    }

    /**
     * Replace current scene.
     */
    protected void replaceScene(String fxmlFile, String title, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/socialapp/frontend/view/" + fxmlFile)
            );
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            log.error("Error replacing scene to {}", fxmlFile, e);
        }
    }

    /**
     * GLOBAL ERROR HANDLING METHODS
     * =============================
     * These methods provide centralized error handling for all controllers.
     * Errors are automatically routed to appropriate handlers with alerts.
     */

    /**
     * Handle any exception with automatic alert.
     * Routes to appropriate handler based on exception type.
     */
    protected void handleException(Exception exception) {
        ErrorHandler.handleException(exception, null);
    }

    /**
     * Handle API exception with automatic alert and routing.
     * Handles:
     * - 401: Session expired → clears session & redirects
     * - 403: Access denied/banned → shows appropriate alert
     * - 4xx: Validation errors → shows user-friendly message
     * - 5xx: Server errors → shows server error alert
     */
    protected void handleApiException(ApiException exception) {
        ErrorHandler.handleApiException(exception, null);
    }

    /**
     * Handle network error (connection, timeout, etc).
     */
    protected void handleNetworkError(Exception exception) {
        ErrorHandler.handleNetworkError(exception, null);
    }

    /**
     * Get current window context for error dialogs.
     * Override in subclasses that have access to controls/stage.
     */
    protected javafx.stage.Window getWindowContext() {
        return null;  // Will be null for generic window context
    }
}
