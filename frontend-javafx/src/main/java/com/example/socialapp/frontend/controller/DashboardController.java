package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.PostDTO;
import com.example.socialapp.frontend.service.RestApiClient;
import com.example.socialapp.frontend.util.ApiException;
import com.example.socialapp.frontend.util.AlertUtils;
import com.example.socialapp.frontend.util.ErrorHandler;
import com.example.socialapp.frontend.util.RoleManager;
import com.example.socialapp.frontend.util.RoleBasedUIRenderer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Feed Controller (Dashboard).
 * Displays posts feed and handles user interactions.
 */
@Slf4j
public class DashboardController extends BaseController implements Initializable {
    @FXML private Label welcomeLabel;
    @FXML private TextArea postContentArea;
    @FXML private Button createPostButton;
    @FXML private VBox postsFeed;
    @FXML private Button logoutButton;
    @FXML private Button moderationButton;
    @FXML private Button adminButton;
    @FXML private Button appealButton;
    @FXML private Button myPostsStatusButton;
    @FXML private Button refreshButton;
    @FXML private Spinner<Integer> pageSpinner;

    private int currentPage = 0;
    private int pageSize = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupWelcomeLabel();
        setupPostCreation();
        setupPagination();
        setupLogout();
        setupMyPostsStatusButton();
        setupModerationButton();
        setupAdminButton();
        setupAppealButton();
        setupRefreshButton();
        loadFeed();
    }

    /**
     * Setup welcome message.
     */
    private void setupWelcomeLabel() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            welcomeLabel.setText("Welcome, " + user.getFirstName() + "!");
        });
    }

    /**
     * Load the feed and display posts as cards.
     */
    private void loadFeed() {
        try {
            Optional<RestApiClient.PagedResponse<?>> response = apiService.getFeed(currentPage, pageSize);
            
            if (response.isPresent()) {
                postsFeed.getChildren().clear();
                RestApiClient.PagedResponse<?> pagedResponse = response.get();
                
                if (pagedResponse.getContent() != null && !pagedResponse.getContent().isEmpty()) {
                    for (Object item : pagedResponse.getContent()) {
                        if (item instanceof PostDTO) {
                            PostDTO post = (PostDTO) item;
                            VBox postCard = createPostCard(post);
                            postsFeed.getChildren().add(postCard);
                        }
                    }
                    log.info("Loaded {} posts from page {}", postsFeed.getChildren().size(), currentPage);
                } else {
                    Label emptyLabel = new Label("No posts available");
                    emptyLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12;");
                    HBox emptyBox = new HBox(emptyLabel);
                    emptyBox.setStyle("-fx-alignment: center; -fx-padding: 40;");
                    postsFeed.getChildren().add(emptyBox);
                }
            } else {
                showAlert("Error", "Failed to load feed");
            }
        } catch (Exception e) {
            log.error("Error loading feed", e);
            handleException(e);
        }
    }
    
    /**
     * Create a card-style VBox for a single post.
     */
    private VBox createPostCard(PostDTO post) {
        VBox card = new VBox();
        card.setStyle("-fx-spacing: 10");
        card.getStyleClass().add("post-card");
        card.setMaxWidth(Double.MAX_VALUE);
        
        // Post Header (Author + Date + Status)
        HBox header = new HBox();
        header.setSpacing(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label author = new Label(post.getAuthorUsername() != null ? post.getAuthorUsername() : "Unknown");
        author.getStyleClass().add("post-author");
        
        Label date = new Label(post.getCreatedAt() != null ? post.getCreatedAt().toString().substring(0, 10) : "N/A");
        date.getStyleClass().add("post-date");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Label statusBadge = new Label(post.getStatus() != null ? post.getStatus().toUpperCase() : "UNKNOWN");
        String badgeClass = getBadgeClass(post.getStatus());
        statusBadge.getStyleClass().addAll("badge", badgeClass);
        
        header.getChildren().addAll(author, date, spacer, statusBadge);
        
        // Post Content - Label with proper width binding
        Label content = new Label(post.getContent() != null ? post.getContent() : "");
        content.getStyleClass().add("post-content");
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        
        card.getChildren().addAll(header, content);
        return card;
    }
    
    /**
     * Get the CSS badge class based on post status.
     */
    private String getBadgeClass(String status) {
        if (status == null) return "badge-pending";
        return switch (status.toUpperCase()) {
            case "PUBLISHED" -> "badge-published";
            case "FLAGGED" -> "badge-flagged";
            case "REMOVED" -> "badge-removed";
            default -> "badge-pending";
        };
    }

    /**
     * Setup post creation functionality.
     */
    private void setupPostCreation() {
        createPostButton.setOnAction(event -> {
            // Check if current user is temporarily or permanently banned
            Optional<com.example.socialapp.frontend.model.UserDTO> currentUser = sessionManager.getCurrentUser();
            if (currentUser.isPresent()) {
                com.example.socialapp.frontend.model.UserDTO user = currentUser.get();
                
                // Check for permanent ban
                if (user.getStatus() != null && user.getStatus().equals("PERM_BANNED")) {
                    log.warn("User {} is permanently banned. Cannot create post.", user.getEmail());
                    String message = user.getBanMessage();
                    if (message == null || message.isEmpty()) {
                        message = "Your account is permanently banned and you cannot post.";
                    }
                    showAlert("Account Permanently Banned", message);
                    return;
                }
                
                // Check for temporary ban
                if (user.getStatus() != null && user.getStatus().equals("TEMP_BANNED")) {
                    log.warn("User {} is temporarily banned. Cannot create post.", user.getEmail());
                    String message = user.getBanMessage();
                    if (message == null || message.isEmpty()) {
                        message = "Your account is temporarily banned. You cannot post at this time.";
                    }
                    showAlert("Account Temporarily Banned", message);
                    return;
                }
            }

            String content = postContentArea.getText();
            if (content.isEmpty()) {
                showAlert("Validation Error", "Post content cannot be empty");
                return;
            }

            try {
                Optional<PostDTO> response = apiService.createPost(content);
                if (response.isPresent()) {
                    PostDTO createdPost = response.get();
                    postContentArea.clear();
                    loadFeed();
                    
                    // Check if post was flagged for moderation
                    if (createdPost.getStatus() != null && 
                        createdPost.getStatus().toString().equalsIgnoreCase("FLAGGED")) {
                        log.info("Post created but flagged for moderation review");
                        showAlert("Content Review Required", 
                            "Your post has been flagged and sent for content review. Our moderation team will review it shortly.");
                    } else {
                        log.info("Post created successfully and published!");
                        showAlert("Success", "Your post has been published successfully!");
                    }
                } else {
                    showAlert("Error", "Failed to create post");
                }
            } catch (ApiException e) {
                if (e.isBanned()) {
                    // Special handling for ban - show detailed ban message
                    log.error("User is banned: {}", e.getErrorMessage());
                    String banMessage = e.getBanMessage();
                    if (banMessage == null || banMessage.isEmpty()) {
                        banMessage = e.getErrorMessage();
                    }
                    showAlert("Account Banned", banMessage);
                } else if (e.isUnauthorized()) {
                    // Special handling for session expired - redirect to login
                    log.error("Session expired: {}", e.getErrorMessage());
                    ErrorHandler.handleApiException(e, createPostButton.getScene().getWindow());
                    try {
                        Stage stage = (Stage) createPostButton.getScene().getWindow();
                        replaceScene("login.fxml", "Social App - Login", stage);
                    } catch (Exception ex) {
                        log.error("Error navigating to login", ex);
                    }
                } else {
                    // Generic error handling
                    handleApiException(e);
                }
            }
        });
    }

    /**
     * Handle banned user - redirect to appeal screen.
     */
    private void handleBannedUser() {
        sessionManager.clearSession();
        try {
            Stage stage = (Stage) createPostButton.getScene().getWindow();
            replaceScene("appeal.fxml", "Social App - Appeal Ban", stage);
        } catch (Exception e) {
            log.error("Error navigating to appeal screen after ban", e);
        }
    }

    /**
     * Handle expired session - redirect to login.
     */
    private void handleSessionExpired() {
        sessionManager.clearSession();
        try {
            Stage stage = (Stage) createPostButton.getScene().getWindow();
            replaceScene("login.fxml", "Social App - Login", stage);
        } catch (Exception e) {
            log.error("Error navigating after session expired", e);
        }
    }

    /**
     * Setup refresh button.
     */
    private void setupRefreshButton() {
        refreshButton.setOnAction(event -> loadFeed());
    }

    /**
     * Setup pagination.
     */
    private void setupPagination() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0);
        pageSpinner.setValueFactory(valueFactory);
        pageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = newVal;
            loadFeed();
        });
    }
    /**
     * Setup logout button.
     */
    private void setupLogout() {
        logoutButton.setOnAction(event -> handleLogout());
    }

    /**
     * Setup moderation button (only for MODERATOR or ADMIN role).
     */
    private void setupModerationButton() {
        Optional<Boolean> canAccess = sessionManager.getCurrentUser()
            .map(user -> RoleManager.canAccessModerationDashboard(Optional.of(user)));

        if (canAccess.orElse(false)) {
            moderationButton.setOnAction(event -> navigateToModeration());
        }
        RoleBasedUIRenderer.showIfModeratorOrAdmin(moderationButton, sessionManager.getCurrentUser());
    }

    /**
     * Navigate to moderation dashboard.
     */
    private void navigateToModeration() {
        try {
            Stage stage = (Stage) moderationButton.getScene().getWindow();
            replaceScene("moderator-dashboard.fxml", "Social App - Moderator Dashboard", stage);
        } catch (Exception e) {
            log.error("Error navigating to moderator dashboard", e);
            showAlert("Error", "Could not navigate to moderator dashboard");
        }
    }

    /**
     * Setup admin button (only for ADMIN role).
     */
    private void setupAdminButton() {
        if (RoleManager.canAccessAdminPanel(sessionManager.getCurrentUser())) {
            adminButton.setOnAction(event -> navigateToAdmin());
        }
        RoleBasedUIRenderer.showIfAdmin(adminButton, sessionManager.getCurrentUser());
    }

    /**
     * Navigate to admin panel.
     */
    private void navigateToAdmin() {
        try {
            Stage stage = (Stage) adminButton.getScene().getWindow();
            replaceScene("admin.fxml", "Social App - Admin Panel", stage);
        } catch (Exception e) {
            log.error("Error navigating to admin panel", e);
            showAlert("Error", "Could not navigate to admin panel");
        }
    }

    /**
     * Setup appeal button (only for banned users).
     */
    private void setupAppealButton() {
        if (RoleManager.isBanned(sessionManager.getCurrentUser())) {
            appealButton.setOnAction(event -> navigateToAppeal());
        }
        RoleBasedUIRenderer.showIfBanned(appealButton, sessionManager.getCurrentUser());
    }

    /**
     * Navigate to appeal screen.
     */
    private void navigateToAppeal() {
        try {
            Stage stage = (Stage) appealButton.getScene().getWindow();
            replaceScene("appeal.fxml", "Social App - Appeal Ban", stage);
        } catch (Exception e) {
            log.error("Error navigating to appeal screen", e);
            showAlert("Error", "Could not navigate to appeal screen");
        }
    }

    /**
     * Setup my posts status button.
     */
    private void setupMyPostsStatusButton() {
        myPostsStatusButton.setOnAction(event -> navigateToMyPostsStatus());
    }

    /**
     * Navigate to my posts status screen.
     */
    private void navigateToMyPostsStatus() {
        try {
            Stage stage = (Stage) myPostsStatusButton.getScene().getWindow();
            replaceScene("my-posts.fxml", "My Posts Status", stage);
        } catch (Exception e) {
            log.error("Error navigating to my posts status screen", e);
            showAlert("Error", "Could not navigate to my posts status screen");
        }
    }

    /**
     * Handle logout.
     */
    private void handleLogout() {
        sessionManager.clearSession();
        apiService.logout();
        log.info("User logged out");

        try {
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            replaceScene("login.fxml", "Social App - Login", stage);
        } catch (Exception e) {
            log.error("Error navigating to login", e);
        }
    }

    /**
     * Show alert dialog.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
