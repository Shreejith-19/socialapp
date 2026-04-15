package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.util.RoleManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Moderator Dashboard Controller.
 * Parent controller for moderator UI with navigation between Flagged Posts and Appeals views.
 * Accessible to users with MODERATOR role.
 */
@Slf4j
public class ModeratorDashboardController extends BaseController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private Button flaggedPostsButton;
    @FXML private Button appealsButton;
    @FXML private BorderPane contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check user has MODERATOR role
        if (!isUserModerator()) {
            showAlert("Access Denied", "You do not have permission to access the moderator dashboard.");
            navigateBack();
            return;
        }

        setupUI();
        setupButtonListeners();
        
        // Load Flagged Posts view by default
        loadFlaggedPostsView();
    }

    /**
     * Check if current user is a moderator.
     */
    private boolean isUserModerator() {
        return RoleManager.isModerator(sessionManager.getCurrentUser()) || 
               RoleManager.isAdmin(sessionManager.getCurrentUser());
    }

    /**
     * Setup UI labels.
     */
    private void setupUI() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            titleLabel.setText("Moderator Dashboard - " + user.getFirstName());
        });
    }

    /**
     * Setup button event listeners for view navigation.
     */
    private void setupButtonListeners() {
        flaggedPostsButton.setOnAction(event -> loadFlaggedPostsView());
        appealsButton.setOnAction(event -> loadAppealsView());
        backButton.setOnAction(event -> navigateBack());
    }

    /**
     * Load the Flagged Posts view into the content area.
     */
    private void loadFlaggedPostsView() {
        try {
            log.info("Loading Flagged Posts view");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/socialapp/frontend/view/flagged-posts.fxml"));
            javafx.scene.layout.VBox flaggedPostsView = loader.load();
            
            // Set the view in the content area
            contentArea.setCenter(flaggedPostsView);
            
            // Style the active button
            updateButtonStyles(true); // true for flagged posts
        } catch (IOException e) {
            log.error("Failed to load flagged posts view", e);
            showAlert("Error", "Failed to load flagged posts view: " + e.getMessage());
        }
    }

    /**
     * Load the Appeals view into the content area.
     */
    private void loadAppealsView() {
        try {
            log.info("Loading Appeals view");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/socialapp/frontend/view/appeals.fxml"));
            javafx.scene.layout.VBox appealsView = loader.load();
            
            // Set the view in the content area
            contentArea.setCenter(appealsView);
            
            // Style the active button
            updateButtonStyles(false); // false for appeals
        } catch (IOException e) {
            log.error("Failed to load appeals view", e);
            showAlert("Error", "Failed to load appeals view: " + e.getMessage());
        }
    }

    /**
     * Update button styles to highlight the active view.
     * @param isFlaggedPostsActive true if flagged posts view is active, false if appeals view is active
     */
    private void updateButtonStyles(boolean isFlaggedPostsActive) {
        String activeStyle = "-fx-padding: 10 20; -fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #007bff;";
        String inactiveStyle = "-fx-padding: 10 20; -fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #333; -fx-background-color: #f0f0f0;";
        
        if (isFlaggedPostsActive) {
            flaggedPostsButton.setStyle(activeStyle);
            appealsButton.setStyle(inactiveStyle);
        } else {
            flaggedPostsButton.setStyle(inactiveStyle);
            appealsButton.setStyle(activeStyle);
        }
    }

    /**
     * Navigate back to the main feed/home screen.
     */
    private void navigateBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/socialapp/frontend/view/dashboard.fxml"));
            BorderPane dashboard = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.getScene().setRoot(dashboard);
            log.info("Navigated back to dashboard");
        } catch (IOException e) {
            log.error("Failed to navigate back", e);
            showAlert("Error", "Failed to return to dashboard: " + e.getMessage());
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
