/**
 * GLOBAL ERROR HANDLING SYSTEM
 * ============================
 * 
 * This document explains the global error handling architecture for the JavaFX frontend.
 * The system provides centralized, consistent error handling across all controllers.
 * 
 * 
 * ARCHITECTURE OVERVIEW
 * =====================
 * 
 * 1. Error Hierarchy:
 *    - ErrorHandler (main dispatcher)
 *      ├─ AlertUtils (UI rendering)
 *      └─ ApiException (API errors)
 *    
 * 2. Error Categories:
 *    - 401: Session Expired → Clears session, shows alert
 *    - 403: Access Denied / Banned → Shows context-appropriate alert
 *    - 4xx: Client Errors → Shows validation/error messages
 *    - 5xx: Server Errors → Shows server error alert
 *    - Network: Connection issues → Shows network error alert
 * 
 * 
 * USAGE PATTERNS
 * ==============
 * 
 * Pattern 1: Try-Catch with Error Handler
 * ----------------------------------------
 * try {
 *     apiService.someMethod();
 * } catch (ApiException e) {
 *     ErrorHandler.handleApiException(e, getWindowContext());
 * } catch (Exception e) {
 *     ErrorHandler.handleException(e, getWindowContext());
 * }
 * 
 * 
 * Pattern 2: Using BaseController Methods (Recommended)
 * -------------------------------------------------------
 * All controllers extend BaseController, which provides helper methods:
 * 
 * try {
 *     apiService.someMethod();
 * } catch (ApiException e) {
 *     handleApiException(e);  // Automatically shows appropriate alert
 * } catch (Exception e) {
 *     handleException(e);     // Routes to correct error handler
 * }
 * 
 * 
 * Pattern 3: Direct Alert Showing
 * --------------------------------
 * showAlert("Title", "Message");           // Info alert
 * showWarning("Title", "Message");         // Warning alert
 * showError("Title", "Message");           // Error alert
 * showValidationError("Validation error"); // Validation alert
 * 
 * 
 * Pattern 4: Custom Error Handling with Callbacks
 * ------------------------------------------------
 * Set a global error callback for custom handling:
 * 
 * ErrorHandler.setGlobalErrorCallback(context -> {
 *     if (context.getType() == ErrorHandler.ErrorType.SESSION_EXPIRED) {
 *         // Custom session expired handling
 *     }
 * });
 * 
 * 
 * LIST OF ERROR TYPES
 * ===================
 * 
 * ErrorType.SESSION_EXPIRED (HTTP 401)
 *   - Cause: JWT token expired or invalid
 *   - Action: Clears session, shows alert
 *   - Recovery: User must log in again
 *   - UI Response: Automatic redirect to login (if callback registered)
 * 
 * ErrorType.ACCESS_DENIED (HTTP 403)
 *   - Cause: User lacks permissions for resource
 *   - Action: Shows "Access Denied" alert
 *   - Recovery: User cannot proceed, must use appropriate role account
 *   - UI Response: Button hidden if role check done properly
 * 
 * ErrorType.USER_BANNED (HTTP 403 with ban context)
 *   - Cause: User account has been banned
 *   - Action: Shows ban alert with context info
 *   - Recovery: User can submit appeal for review
 *   - UI Response: Redirects to appeal screen
 * 
 * ErrorType.VALIDATION_ERROR (HTTP 4xx)
 *   - Cause: Invalid input or request format
 *   - Action: Shows validation error message
 *   - Recovery: User corrects input and retries
 *   - UI Response: Form remains on screen for user to fix
 * 
 * ErrorType.NOT_FOUND (HTTP 404)
 *   - Cause: Requested resource doesn't exist
 *   - Action: Shows "Not Found" error
 *   - Recovery: User must navigate to existing resource
 *   - UI Response: May navigate back or to home page
 * 
 * ErrorType.SERVER_ERROR (HTTP 5xx)
 *   - Cause: Backend server error
 *   - Action: Shows server error alert with status code
 *   - Recovery: Suggest retry later, contact support
 *   - UI Response: Operation fails, user can retry
 * 
 * ErrorType.NETWORK_ERROR (Connection issues)
 *   - Cause: Network unreachable, timeout, socket error
 *   - Action: Shows network error alert
 *   - Recovery: Check internet, retry when connection restored
 *   - UI Response: Suggests checking connection
 * 
 * ErrorType.UNKNOWN_ERROR (Uncategorized)
 *   - Cause: Unexpected error not in other categories
 *   - Action: Shows generic error alert with message
 *   - Recovery: Retry or contact support
 *   - UI Response: Shows error message from exception
 * 
 * 
 * ALERT TYPES
 * ===========
 * 
 * AlertUtils provides these methods:
 * 
 * showInfo(title, message)           → Blue info dialog
 * showWarning(title, message)        → Orange warning dialog
 * showError(title, message)          → Red error dialog
 * showValidationError(message)       → Validation message
 * showSessionExpired()               → Session timeout alert
 * showAccessDenied()                 → Permission denied alert
 * showUserBanned(reason)             → Ban notice with reason
 * showNetworkError()                 → Connection error alert
 * showNetworkError(customMessage)    → Connection error with custom text
 * showServerError()                  → Server error alert
 * showServerError(statusCode)        → Server error with HTTP code
 * 
 * 
 * EXAMPLE IMPLEMENTATIONS
 * =======================
 * 
 * Example 1: Login Controller Error Handling
 * -------------------------------------------
 * private void handleLogin() {
 *     try {
 *         AuthResponse response = apiService.login(email, password).orElseThrow();
 *         sessionManager.setSession(response.getUser(), response.getToken());
 *         navigateTo("dashboard.fxml");
 *     } catch (ApiException e) {
 *         if (e.getStatusCode() == 401) {
 *             showError("Login Failed", "Invalid email or password");
 *         } else {
 *             handleApiException(e);  // Generic API error handling
 *         }
 *     }
 * }
 * 
 * 
 * Example 2: Feed Loading with Comprehensive Error Handling
 * ----------------------------------------------------------
 * private void loadFeed() {
 *     try {
 *         Optional<PagedResponse<?>> response = apiService.getFeed(page, size);
 *         if (response.isPresent()) {
 *             displayFeed(response.get());
 *         } else {
 *             showWarning("No Data", "No posts available");
 *         }
 *     } catch (ApiException e) {
 *         handleApiException(e);  // Handles 401, 403, etc.
 *     } catch (Exception e) {
 *         handleException(e);     // Handles network, other errors
 *     }
 * }
 * 
 * 
 * Example 3: Post Creation with Ban Detection
 * --------------------------------------------
 * private void createPost() {
 *     try {
 *         PostDTO post = apiService.createPost(content).orElseThrow();
 *         showAlert("Success", "Post created!");
 *         loadFeed();
 *     } catch (ApiException e) {
 *         if (e.isBanned()) {
 *             AlertUtils.showUserBanned(e.getErrorMessage());
 *             navigateTo("appeal.fxml");
 *         } else if (e.isUnauthorized()) {
 *             handleSessionExpired();
 *         } else {
 *             handleApiException(e);
 *         }
 *     }
 * }
 * 
 * 
 * Example 4: Global Error Callback Setup
 * ----------------------------------------
 * In your application initialization:
 * 
 * ErrorHandler.setGlobalErrorCallback(context -> {
 *     ErrorHandler.ErrorType type = context.getType();
 *     
 *     // Handle session expired globally
 *     if (type == ErrorHandler.ErrorType.SESSION_EXPIRED) {
 *         sessionManager.clearSession();
 *         window.switchToLoginScreen();
 *     }
 *     
 *     // Log all errors
 *     log.error("Global error occurred: {} ({})", 
 *         type, context.getStatusCode());
 * });
 * 
 * 
 * BEST PRACTICES
 * ==============
 * 
 * 1. Always use BaseController methods (handleApiException, handleException, showAlert)
 *    They provide consistent error handling across the app
 * 
 * 2. Catch specific exceptions first, then use error handlers:
 *    - Catch ApiException for API errors
 *    - Catch generic Exception for other errors
 * 
 * 3. Don't show multiple alerts for the same error
 *    - ErrorHandler already shows appropriate alerts
 *    - Don't wrap in another try-catch that shows another alert
 * 
 * 4. Use meaningful error messages
 *    - From backend: Use apiException.getErrorMessage()
 *    - Custom: Create descriptive messages for users
 * 
 * 5. Handle context-specific errors specially
 *    - Ban detection: Use isBanned() check
 *    - Session expired: Use isUnauthorized() check
 *    - Then redirect or show context-appropriate UI
 * 
 * 6. Register global error callback for app-wide handling
 *    - Setup during app initialization
 *    - Handle session expired redirects globally
 *    - Log all errors for debugging
 * 
 * 7. Test error scenarios
 *    - Simulate 401: Expire token manually
 *    - Simulate 403: Trigger ban or permission denial
 *    - Simulate network: Disconnect network or use proxy
 *    - Simulate 5xx: Use mock server error response
 * 
 * 
 * CONTROLLER INTEGRATION CHECKLIST
 * ================================
 * 
 * When adding error handling to a controller:
 * 
 * □ Import: ApiException, ErrorHandler, AlertUtils
 * □ Wrap API calls in try-catch
 * □ Catch ApiException first
 * □ Catch generic Exception second
 * □ Call handleApiException() for API errors
 * □ Call handleException() for other errors
 * □ Don't swallow exceptions - always log or handle
 * □ Show user-friendly messages, not technical details
 * □ Handle special cases (ban, session expired) separately
 * □ Test with different error codes (401, 403, 500, etc.)
 */
