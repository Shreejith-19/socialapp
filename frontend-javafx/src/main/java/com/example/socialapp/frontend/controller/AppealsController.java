package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.AppealDTO;
import com.example.socialapp.frontend.service.ApiService;
import com.example.socialapp.frontend.service.RestApiClient;
import com.example.socialapp.frontend.util.ApiException;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Appeals Controller.
 * Handles display and moderation of user appeals (ban and post appeals).
 * Accessible to MODERATOR and ADMIN users.
 * Embedded within ModeratorDashboard view.
 */
@Slf4j
public class AppealsController implements Initializable {
    @FXML private TableView<AppealDTO> appealsTable;
    @FXML private TableColumn<AppealDTO, String> userColumn;
    @FXML private TableColumn<AppealDTO, String> appealTypeColumn;
    @FXML private TableColumn<AppealDTO, String> reasonColumn;
    @FXML private TableColumn<AppealDTO, String> createdAtColumn;
    @FXML private TableColumn<AppealDTO, String> statusColumn;
    @FXML private TextArea decisionReasonArea;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Spinner<Integer> pageSpinner;
    @FXML private Label appealCountLabel;

    private int currentPage = 0;
    private int pageSize = 20;
    private AppealDTO selectedAppeal = null;
    
    // API Service instance
    private ApiService apiService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            this.apiService = ApiService.getInstance();
        } catch (Exception e) {
            log.warn("Could not initialize ApiService, will be set by parent controller");
        }

        setupTableColumns();
        setupPagination();
        setupDecisionButtons();
        setupTableSelection();
        loadAppeals();
    }

    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        userColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getUsername() != null ? 
                cellData.getValue().getUsername() : "Unknown"));
        
        appealTypeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getAppealType() != null ? 
                cellData.getValue().getAppealType().toString() : "UNKNOWN"));
        
        reasonColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getReason()));
        
        createdAtColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().toLocalDate().toString() : "N/A"));
        
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));

        appealsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
            loadAppeals();
        });
    }

    /**
     * Setup table row selection.
     */
    private void setupTableSelection() {
        appealsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedAppeal = newVal;
            if (newVal != null) {
                decisionReasonArea.setDisable(false);
                approveButton.setDisable(false);
                rejectButton.setDisable(false);
            } else {
                decisionReasonArea.clear();
                decisionReasonArea.setDisable(true);
                approveButton.setDisable(true);
                rejectButton.setDisable(true);
            }
        });
    }

    /**
     * Setup decision buttons.
     */
    private void setupDecisionButtons() {
        approveButton.setDisable(true);
        rejectButton.setDisable(true);

        approveButton.setOnAction(event -> submitAppealDecision(true));
        rejectButton.setOnAction(event -> submitAppealDecision(false));
    }

    /**
     * Submit appeal decision (approve or reject).
     * @param approved true to approve, false to reject
     */
    private void submitAppealDecision(boolean approved) {
        if (selectedAppeal == null) {
            showAlert("Error", "No appeal selected");
            return;
        }

        String decision = decisionReasonArea.getText();
        
        if (decision.trim().isEmpty()) {
            showAlert("Error", "Please provide a reason for your decision");
            return;
        }
        
        try {
            // Call the decideAppeal API endpoint
            boolean success = apiService.decideAppeal(
                selectedAppeal.getId().toString(),
                approved ? "APPROVED" : "REJECTED",
                decision
            );

            if (success) {
                String result = approved ? "APPROVED" : "REJECTED";
                log.info("Appeal {} submitted for appeal {}", result, selectedAppeal.getId());
                
                // If appeal was approved, refresh the current user session
                if (approved) {
                    try {
                        apiService.refreshCurrentUser();
                        log.info("User session refreshed after appeal approval");
                    } catch (Exception e) {
                        log.warn("Could not refresh user session: {}", e.getMessage());
                    }
                }
                
                showAlert("Success", "Appeal " + result + " successfully");
                decisionReasonArea.clear();
                appealsTable.getSelectionModel().clearSelection();
                loadAppeals(); // Refresh list
            } else {
                showAlert("Error", "Failed to submit appeal decision");
            }
        } catch (ApiException e) {
            if (e.isUnauthorized() || e.isBanned()) {
                log.error("Unauthorized to submit appeal decision: {}", e.getErrorMessage());
                showAlert("Access Denied", e.getErrorMessage());
            } else {
                showAlert("Error", e.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error submitting appeal decision", e);
            showAlert("Error", "Error submitting decision: " + e.getMessage());
        }
    }

    /**
     * Load pending appeals.
     */
    private void loadAppeals() {
        try {
            Optional<RestApiClient.PagedResponse<?>> response = apiService.getAppeals(currentPage, pageSize);
            
            if (response.isPresent()) {
                appealsTable.getItems().clear();
                RestApiClient.PagedResponse<?> pagedResponse = response.get();
                
                if (pagedResponse.getContent() != null) {
                    int count = 0;
                    for (Object item : pagedResponse.getContent()) {
                        if (item instanceof AppealDTO) {
                            AppealDTO appeal = (AppealDTO) item;
                            // Only show pending appeals
                            if ("PENDING".equals(appeal.getStatus())) {
                                appealsTable.getItems().add(appeal);
                                count++;
                            }
                        }
                    }
                    
                    appealCountLabel.setText("Appeals: " + count + " / " + pagedResponse.getTotalElements());
                    log.info("Loaded {} pending appeals from page {}", count, currentPage);
                }
            } else {
                showAlert("Error", "Failed to load appeals");
            }
        } catch (Exception e) {
            log.error("Error loading appeals", e);
            showAlert("Error", "Error loading appeals: " + e.getMessage());
        }
    }

    /**
     * Inject the ApiService instance.
     */
    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
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
