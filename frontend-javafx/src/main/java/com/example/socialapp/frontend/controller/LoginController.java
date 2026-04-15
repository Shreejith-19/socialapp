package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.AuthResponse;
import com.example.socialapp.frontend.model.UserDTO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Login Controller.
 * Handles user authentication.
 */
@Slf4j
public class LoginController extends BaseController implements Initializable {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loginButton.setOnAction(event -> handleLogin());
        registerButton.setOnAction(event -> handleRegister());
    }

    /**
     * Handle login action.
     */
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Email and password are required");
            return;
        }

        log.info("Attempting login for: {}", email);
        Optional<AuthResponse> response = apiService.login(email, password);
        
        if (response.isPresent()) {
            // Store JWT token in API service for future requests
            log.info("Login successful. Token response present. Setting token on RestApiClient...");
            log.info("ApiService instance: {}", apiService);
            log.info("RestApiClient instance: {}", apiService.getRestClient());
            apiService.getRestClient().setAuthToken(response.get().getToken());
            log.info("Token set. Now fetching current user...");
            
            // Get current user
            Optional<UserDTO> user = apiService.getCurrentUser();
            if (user.isPresent()) {
                // Check if user is permanently banned (not allowed to login)
                // Temporary ban is allowed - user can login but can't create posts
                if (user.get().getStatus() != null && 
                    user.get().getStatus().equals("PERM_BANNED")) {
                    log.warn("User {} is permanently banned: {}", email, user.get().getBanMessage());
                    String banMessage = user.get().getBanMessage();
                    showAlert("Account Permanently Banned", banMessage != null && !banMessage.isEmpty() 
                        ? banMessage 
                        : "Your account has been permanently banned. Please contact support.");
                    emailField.clear();
                    passwordField.clear();
                    apiService.getRestClient().clearAuthToken();
                    return;
                }
                
                // Store session (allows temp banned users to login)
                sessionManager.setSession(user.get(), response.get().getToken());
                log.info("User logged in successfully: {}", email);
                navigateToFeed();
            } else {
                log.error("Could not retrieve user information after successful login");
                showAlert("Error", "Could not retrieve user information");
            }
        } else {
            showAlert("Login Failed", "Invalid email or password");
            passwordField.clear();
        }
    }

    /**
     * Handle register action.
     */
    private void handleRegister() {
        try {
            Stage stage = (Stage) registerButton.getScene().getWindow();
            replaceScene("signup.fxml", "Social App - Signup", stage);
        } catch (Exception e) {
            log.error("Error navigating to signup", e);
            showAlert("Error", "Could not navigate to signup screen");
        }
    }

    /**
     * Navigate to feed after successful login.
     */
    private void navigateToFeed() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            replaceScene("dashboard.fxml", "Social App - Feed", stage);
        } catch (Exception e) {
            log.error("Error navigating to feed", e);
            showAlert("Error", "Could not navigate to feed screen");
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
