package com.example.socialapp.frontend.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

/**
 * Alert Utility for displaying consistent alert dialogs.
 * Provides methods for showing different types of alerts with proper styling.
 */
@Slf4j
public class AlertUtils {
    /**
     * Show an information alert dialog.
     */
    public static void showInfo(String title, String message) {
        showAlert(AlertType.INFORMATION, title, message, null);
    }

    /**
     * Show an information alert dialog with owner window.
     */
    public static void showInfo(String title, String message, Window owner) {
        showAlert(AlertType.INFORMATION, title, message, owner);
    }

    /**
     * Show a warning alert dialog.
     */
    public static void showWarning(String title, String message) {
        showAlert(AlertType.WARNING, title, message, null);
    }

    /**
     * Show a warning alert dialog with owner window.
     */
    public static void showWarning(String title, String message, Window owner) {
        showAlert(AlertType.WARNING, title, message, owner);
    }

    /**
     * Show an error alert dialog.
     */
    public static void showError(String title, String message) {
        showAlert(AlertType.ERROR, title, message, null);
    }

    /**
     * Show an error alert dialog with owner window.
     */
    public static void showError(String title, String message, Window owner) {
        showAlert(AlertType.ERROR, title, message, owner);
    }

    /**
     * Show a confirmation alert dialog.
     * Returns true if user clicks OK, false otherwise.
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().isPresent();
    }

    /**
     * Show an alert for session expired (401).
     */
    public static void showSessionExpired() {
        showAlert(AlertType.WARNING,
            "Session Expired",
            "Your session has expired. Please log in again.",
            null);
        log.warn("Session expired alert shown");
    }

    /**
     * Show an alert for access denied (403).
     */
    public static void showAccessDenied() {
        showAlert(AlertType.ERROR,
            "Access Denied",
            "You do not have permission to access this resource.",
            null);
        log.warn("Access denied alert shown");
    }

    /**
     * Show an alert for ban (403 with ban context).
     */
    public static void showUserBanned(String reason) {
        String message = "You have been banned from this platform.";
        if (reason != null && !reason.isEmpty()) {
            message += "\n\nReason: " + reason;
        }
        message += "\n\nYou can submit an appeal to request a review of this decision.";
        
        showAlert(AlertType.ERROR,
            "Account Banned",
            message,
            null);
        log.warn("User banned alert shown");
    }

    /**
     * Show an alert for network error.
     */
    public static void showNetworkError() {
        showAlert(AlertType.ERROR,
            "Network Error",
            "Could not connect to the server. Please check your internet connection and try again.",
            null);
        log.error("Network error alert shown");
    }

    /**
     * Show an alert for network error with custom message.
     */
    public static void showNetworkError(String customMessage) {
        String message = "Network Error: " + customMessage + "\n\nPlease check your connection and try again.";
        showAlert(AlertType.ERROR,
            "Network Error",
            message,
            null);
        log.error("Network error alert shown: {}", customMessage);
    }

    /**
     * Show an alert for server error (5xx).
     */
    public static void showServerError() {
        showAlert(AlertType.ERROR,
            "Server Error",
            "The server encountered an error. Please try again later.",
            null);
        log.error("Server error alert shown");
    }

    /**
     * Show an alert for server error with status code.
     */
    public static void showServerError(int statusCode) {
        String message = "Server Error (HTTP " + statusCode + ").\n\nPlease try again later.";
        showAlert(AlertType.ERROR,
            "Server Error",
            message,
            null);
        log.error("Server error alert shown: {}", statusCode);
    }

    /**
     * Show an alert for validation error.
     */
    public static void showValidationError(String message) {
        showAlert(AlertType.WARNING,
            "Validation Error",
            message,
            null);
        log.warn("Validation error alert shown: {}", message);
    }

    /**
     * Show a generic error alert.
     */
    public static void showError(String title, String message, Throwable throwable) {
        String details = "";
        if (throwable != null && throwable.getMessage() != null) {
            details = "\n\nDetails: " + throwable.getMessage();
        }
        showAlert(AlertType.ERROR,
            title,
            message + details,
            null);
        log.error("Error alert shown: {} - {}", title, message, throwable);
    }

    /**
     * Show an alert with custom styling.
     */
    private static void showAlert(AlertType type, String title, String message, Window owner) {
        Alert alert = new Alert(type);
        
        if (owner != null) {
            alert.initOwner(owner);
        }
        
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Apply styling based on alert type
        switch (type) {
            case ERROR:
                alert.getDialogPane().setStyle("-fx-text-fill: #d9534f;");
                break;
            case WARNING:
                alert.getDialogPane().setStyle("-fx-text-fill: #ff9800;");
                break;
            case INFORMATION:
                alert.getDialogPane().setStyle("-fx-text-fill: #0275d8;");
                break;
            default:
                break;
        }
        
        alert.showAndWait();
    }
}
