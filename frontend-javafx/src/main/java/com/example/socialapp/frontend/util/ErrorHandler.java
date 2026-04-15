package com.example.socialapp.frontend.util;

import javafx.application.Platform;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;

/**
 * Global Error Handler for consistent error management.
 * Handles API errors, network errors, and other exceptions.
 * 
 * Error Categories:
 * - 401: Session Expired → Redirect to login
 * - 403: Access Denied or User Banned → Handle appropriately
 * - 4xx: Client Errors → Show validation/user errors
 * - 5xx: Server Errors → Show server error message
 * - Network: Connection errors → Show network error
 */
@Slf4j
public class ErrorHandler {
    private static SessionManager sessionManager = SessionManager.getInstance();
    private static ErrorCallback globalErrorCallback;

    /**
     * Functional interface for custom error handling.
     */
    @FunctionalInterface
    public interface ErrorCallback {
        void onError(ErrorContext context);
    }

    /**
     * Error context containing all error information.
     */
    public static class ErrorContext {
        private final ErrorType type;
        private final int statusCode;
        private final String message;
        private final Throwable throwable;
        private final Window owner;

        public ErrorContext(ErrorType type, int statusCode, String message, Throwable throwable, Window owner) {
            this.type = type;
            this.statusCode = statusCode;
            this.message = message;
            this.throwable = throwable;
            this.owner = owner;
        }

        public ErrorType getType() { return type; }
        public int getStatusCode() { return statusCode; }
        public String getMessage() { return message; }
        public Throwable getThrowable() { return throwable; }
        public Window getOwner() { return owner; }
    }

    /**
     * Error type categories.
     */
    public enum ErrorType {
        SESSION_EXPIRED,      // 401
        ACCESS_DENIED,        // 403
        USER_BANNED,          // 403 with ban context
        VALIDATION_ERROR,     // 4xx (400, 422, etc.)
        NOT_FOUND,            // 404
        SERVER_ERROR,         // 5xx
        NETWORK_ERROR,        // Connection issues
        UNKNOWN_ERROR         // Unclassified
    }

    /**
     * Register a global error callback for custom handling.
     */
    public static void setGlobalErrorCallback(ErrorCallback callback) {
        globalErrorCallback = callback;
    }

    /**
     * Handle an ApiException.
     * Automatically routes to appropriate handler based on status code.
     */
    public static void handleApiException(ApiException exception) {
        handleApiException(exception, null);
    }

    /**
     * Handle an ApiException with owner window.
     */
    public static void handleApiException(ApiException exception, Window owner) {
        log.error("API Exception: {} - {}", exception.getStatusCode(), exception.getErrorMessage());

        int code = exception.getStatusCode();
        String message = exception.getErrorMessage();

        if (code == 401) {
            handleSessionExpired(message, owner);
        } else if (code == 403) {
            handleAccessDenied(message, owner);
        } else if (code >= 400 && code < 500) {
            handleClientError(code, message, owner);
        } else if (code >= 500) {
            handleServerError(code, message, owner);
        } else {
            handleUnknownError(code, message, owner);
        }
    }

    /**
     * Handle session expired (401).
     * Clears session and redirects to login.
     */
    private static void handleSessionExpired(String message, Window owner) {
        log.warn("Session expired (401)");
        
        // Execute on JavaFX thread
        Platform.runLater(() -> {
            AlertUtils.showSessionExpired();
            
            // Clear session
            sessionManager.clearSession();
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                globalErrorCallback.onError(new ErrorContext(
                    ErrorType.SESSION_EXPIRED, 401, message, null, owner
                ));
            }
        });
    }

    /**
     * Handle access denied (403).
     * Could be ban or general access denied.
     */
    private static void handleAccessDenied(String message, Window owner) {
        log.warn("Access denied (403)");
        
        Platform.runLater(() -> {
            // Check if it's a ban message
            if (message != null && message.toLowerCase().contains("ban")) {
                AlertUtils.showUserBanned(message);
            } else {
                AlertUtils.showAccessDenied();
            }
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                ErrorType type = (message != null && message.toLowerCase().contains("ban")) 
                    ? ErrorType.USER_BANNED 
                    : ErrorType.ACCESS_DENIED;
                globalErrorCallback.onError(new ErrorContext(
                    type, 403, message, null, owner
                ));
            }
        });
    }

    /**
     * Handle client error (4xx).
     */
    private static void handleClientError(int statusCode, String message, Window owner) {
        log.warn("Client error ({}): {}", statusCode, message);
        
        Platform.runLater(() -> {
            if (statusCode == 404) {
                AlertUtils.showError("Not Found", "The requested resource was not found.", owner);
            } else if (statusCode == 422) {
                AlertUtils.showValidationError(message != null ? message : "Please check your input and try again.");
            } else {
                AlertUtils.showValidationError(message != null ? message : "Invalid request. Please try again.");
            }
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                globalErrorCallback.onError(new ErrorContext(
                    statusCode == 404 ? ErrorType.NOT_FOUND : ErrorType.VALIDATION_ERROR,
                    statusCode, message, null, owner
                ));
            }
        });
    }

    /**
     * Handle server error (5xx).
     */
    private static void handleServerError(int statusCode, String message, Window owner) {
        log.error("Server error ({}): {}", statusCode, message);
        
        Platform.runLater(() -> {
            AlertUtils.showServerError(statusCode);
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                globalErrorCallback.onError(new ErrorContext(
                    ErrorType.SERVER_ERROR, statusCode, message, null, owner
                ));
            }
        });
    }

    /**
     * Handle unknown HTTP error.
     */
    private static void handleUnknownError(int statusCode, String message, Window owner) {
        log.error("Unknown error ({}): {}", statusCode, message);
        
        Platform.runLater(() -> {
            AlertUtils.showError(
                "Error (HTTP " + statusCode + ")",
                message != null ? message : "An unexpected error occurred. Please try again.",
                owner
            );
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                globalErrorCallback.onError(new ErrorContext(
                    ErrorType.UNKNOWN_ERROR, statusCode, message, null, owner
                ));
            }
        });
    }

    /**
     * Handle network error (connection issues).
     */
    public static void handleNetworkError(Throwable throwable) {
        handleNetworkError(throwable, null);
    }

    /**
     * Handle network error with owner window.
     */
    public static void handleNetworkError(Throwable throwable, Window owner) {
        log.error("Network error", throwable);
        
        Platform.runLater(() -> {
            String message = throwable != null ? throwable.getMessage() : null;
            
            if (message != null && !message.isEmpty()) {
                AlertUtils.showNetworkError(message);
            } else {
                AlertUtils.showNetworkError();
            }
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                globalErrorCallback.onError(new ErrorContext(
                    ErrorType.NETWORK_ERROR, 0, message, throwable, owner
                ));
            }
        });
    }

    /**
     * Handle any exception with context.
     */
    public static void handleException(Exception exception) {
        handleException(exception, null);
    }

    /**
     * Handle any exception with window context.
     */
    public static void handleException(Exception exception, Window owner) {
        log.error("Exception occurred", exception);
        
        // Check if it's an ApiException
        if (exception instanceof ApiException) {
            handleApiException((ApiException) exception, owner);
            return;
        }

        // Check for network-related exceptions
        String exceptionClass = exception.getClass().getName();
        if (exceptionClass.contains("ConnectException") || 
            exceptionClass.contains("SocketException") ||
            exceptionClass.contains("UnknownHostException") ||
            exceptionClass.contains("TimeoutException")) {
            handleNetworkError(exception, owner);
            return;
        }

        // Handle as generic exception
        Platform.runLater(() -> {
            AlertUtils.showError(
                "Error",
                "An unexpected error occurred: " + exception.getMessage(),
                exception
            );
            
            // Trigger optional callback
            if (globalErrorCallback != null) {
                globalErrorCallback.onError(new ErrorContext(
                    ErrorType.UNKNOWN_ERROR, 0, exception.getMessage(), exception, owner
                ));
            }
        });
    }

    /**
     * Log an error without showing alert.
     */
    public static void logError(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    /**
     * Reset error handler.
     */
    public static void reset() {
        globalErrorCallback = null;
        log.info("Error handler reset");
    }
}
