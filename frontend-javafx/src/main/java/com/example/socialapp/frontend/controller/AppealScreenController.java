package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.AppealDTO;
import com.example.socialapp.frontend.util.ApiException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Appeal Screen Controller.
 * Allows banned users to submit appeals and view appeal status.
 */
@Slf4j
public class AppealScreenController extends BaseController implements Initializable {
    @FXML private Label titleLabel;
    @FXML private Label banReasonLabel;
    @FXML private TextArea appealReasonArea;
    @FXML private Button submitAppealButton;
    @FXML private Button backButton;

    @FXML private TableView<AppealDTO> appealsTable;
    @FXML private TableColumn<AppealDTO, String> appealIdColumn;
    @FXML private TableColumn<AppealDTO, String> reasonColumn;
    @FXML private TableColumn<AppealDTO, String> statusColumn;
    @FXML private TableColumn<AppealDTO, String> createdAtColumn;

    @FXML private Label noAppealsLabel;
    @FXML private Label submittedMessageLabel;

    private String banId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupTableColumns();
        setupSubmitButton();
        setupBackButton();
        loadAppeals();
        
        // Hide success message initially
        submittedMessageLabel.setVisible(false);
    }

    /**
     * Setup UI labels.
     */
    private void setupUI() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            titleLabel.setText("Ban Appeal - " + user.getFirstName());
        });

        // For demo: show a placeholder ban reason
        // In production, this would come from the banned endpoint
        banReasonLabel.setText("Your account has been temporarily banned due to policy violation.");
    }

    /**
     * Setup appeals table columns.
     */
    private void setupTableColumns() {
        appealIdColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId().toString().substring(0, 8) + "..."));
        
        reasonColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReason()));
        
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        
        createdAtColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().toLocalDate().toString() : "N/A"));

        appealsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Setup submit button.
     */
    private void setupSubmitButton() {
        submitAppealButton.setOnAction(event -> submitAppeal());
    }

    /**
     * Submit a new appeal.
     */
    private void submitAppeal() {
        String reason = appealReasonArea.getText().trim();

        // Validation
        if (reason.isEmpty()) {
            showAlert("Validation Error", "Please provide a reason for your appeal");
            return;
        }

        if (reason.length() < 10) {
            showAlert("Validation Error", "Appeal reason must be at least 10 characters");
            return;
        }

        if (reason.length() > 1000) {
            showAlert("Validation Error", "Appeal reason cannot exceed 1000 characters");
            return;
        }

        try {
            // Get the user's most recent ban ID
            // In a real app, this would come from the backend
            Optional<AppealDTO> response = apiService.submitAppeal("ban-id-placeholder", reason);

            if (response.isPresent()) {
                log.info("Appeal submitted successfully");
                
                // Show success message
                submittedMessageLabel.setVisible(true);
                submittedMessageLabel.setText("✓ Appeal submitted successfully. Our team will review it shortly.");
                submittedMessageLabel.setStyle("-fx-text-fill: #5cb85c; -fx-font-weight: bold;");
                
                // Clear input
                appealReasonArea.clear();
                
                // Reload appeals to show the new one
                loadAppeals();
                
                // Hide success message after 5 seconds
                new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            javafx.application.Platform.runLater(() -> {
                                submittedMessageLabel.setVisible(false);
                            });
                        }
                    },
                    5000
                );
            } else {
                showAlert("Error", "Failed to submit appeal");
            }
        } catch (Exception e) {
            log.error("Error submitting appeal: {}", e.getMessage());
            showAlert("Error", "Error submitting appeal: " + e.getMessage());
        }
    }

    /**
     * Load user's appeals.
     */
    private void loadAppeals() {
        try {
            // In a real app, you'd have an endpoint to get user's appeals
            // For now, we'll show an empty table with explanation
            appealsTable.getItems().clear();
            
            // TODO: Call apiService.getUserAppeals() when backend endpoint is available
            // Example:
            // Optional<List<AppealDTO>> response = apiService.getUserAppeals();
            // if (response.isPresent()) {
            //     appealsTable.getItems().addAll(response.get());
            // }

            if (appealsTable.getItems().isEmpty()) {
                noAppealsLabel.setVisible(true);
                noAppealsLabel.setText("No appeals submitted yet. Submit your first appeal above.");
            } else {
                noAppealsLabel.setVisible(false);
            }

            log.info("Loaded {} appeals", appealsTable.getItems().size());
        } catch (Exception e) {
            log.error("Error loading appeals", e);
            showAlert("Error", "Could not load appeals: " + e.getMessage());
        }
    }

    /**
     * Setup back button.
     */
    private void setupBackButton() {
        backButton.setOnAction(event -> navigateBack());
    }

    /**
     * Navigate back to login.
     */
    private void navigateBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            replaceScene("login.fxml", "Social App - Login", stage);
        } catch (Exception e) {
            log.error("Error navigating back", e);
            showAlert("Error", "Could not navigate back");
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
