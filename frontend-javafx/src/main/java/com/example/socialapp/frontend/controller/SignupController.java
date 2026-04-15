package com.example.socialapp.frontend.controller;

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
 * Signup Controller.
 * Handles user registration.
 */
@Slf4j
public class SignupController extends BaseController implements Initializable {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupButton;
    @FXML private Button loginButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        signupButton.setOnAction(event -> handleSignup());
        loginButton.setOnAction(event -> handleBackToLogin());
    }

    /**
     * Handle signup action.
     */
    private void handleSignup() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Validation Error", "Passwords do not match");
            confirmPasswordField.clear();
            return;
        }

        if (password.length() < 6) {
            showAlert("Validation Error", "Password must be at least 6 characters");
            return;
        }

        log.info("Attempting signup for: {}", email);
        Optional<UserDTO> response = apiService.signup(email, password, firstName, lastName);

        if (response.isPresent()) {
            showAlert("Success", "Account created successfully! Please login.");
            navigateToLogin();
        } else {
            showAlert("Signup Failed", "Could not create account. Email may already be in use.");
        }
    }

    /**
     * Handle back to login action.
     */
    private void handleBackToLogin() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            replaceScene("login.fxml", "Social App - Login", stage);
        } catch (Exception e) {
            log.error("Error navigating back to login", e);
            showAlert("Error", "Could not navigate to login screen");
        }
    }

    /**
     * Navigate to login after successful signup.
     */
    private void navigateToLogin() {
        try {
            Stage stage = (Stage) signupButton.getScene().getWindow();
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
