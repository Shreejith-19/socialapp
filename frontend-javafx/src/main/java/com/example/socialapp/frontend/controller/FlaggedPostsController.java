package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.ModerationDecisionDTO;
import com.example.socialapp.frontend.model.PostDTO;
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
 * Flagged Posts Controller.
 * Handles display and moderation of flagged posts.
 * Embedded within ModeratorDashboard view.
 */
@Slf4j
public class FlaggedPostsController implements Initializable {
    @FXML private TableView<PostDTO> flaggedPostsTable;
    @FXML private TableColumn<PostDTO, String> authorColumn;
    @FXML private TableColumn<PostDTO, String> contentColumn;
    @FXML private TableColumn<PostDTO, String> statusColumn;
    @FXML private TableColumn<PostDTO, String> dateColumn;
    @FXML private TextArea decisionReasonArea;
    @FXML private Button approveButton;
    @FXML private Button removeButton;
    @FXML private Button escalateButton;
    @FXML private Spinner<Integer> pageSpinner;
    @FXML private Label postCountLabel;

    private int currentPage = 0;
    private int pageSize = 20;
    private PostDTO selectedPost = null;
    
    // These will be injected by the parent or retrieved from static context
    private ApiService apiService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize the API service from a static context or parent
        // For now, we'll get it from the application context
        try {
            // This would typically be injected, but for now we'll use a singleton or static accessor
            // In a real app, use dependency injection
            this.apiService = ApiService.getInstance(); // Assuming singleton pattern
        } catch (Exception e) {
            log.warn("Could not initialize ApiService, will be set by parent controller");
        }

        setupTableColumns();
        setupPagination();
        setupDecisionButtons();
        setupTableSelection();
        loadModerationQueue();
    }

    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        authorColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAuthorUsername()));
        
        contentColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSummary()));
        
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        
        dateColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().toLocalDate().toString() : "N/A"));

        flaggedPostsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
            loadModerationQueue();
        });
    }

    /**
     * Setup table row selection.
     */
    private void setupTableSelection() {
        flaggedPostsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedPost = newVal;
            if (newVal != null) {
                decisionReasonArea.setDisable(false);
                approveButton.setDisable(false);
                removeButton.setDisable(false);
                escalateButton.setDisable(false);
            } else {
                decisionReasonArea.clear();
                decisionReasonArea.setDisable(true);
                approveButton.setDisable(true);
                removeButton.setDisable(true);
                escalateButton.setDisable(true);
            }
        });
    }

    /**
     * Setup decision buttons.
     */
    private void setupDecisionButtons() {
        approveButton.setDisable(true);
        removeButton.setDisable(true);
        escalateButton.setDisable(true);

        approveButton.setOnAction(event -> submitDecision("APPROVED"));
        removeButton.setOnAction(event -> submitDecision("REMOVED"));
        escalateButton.setOnAction(event -> submitDecision("ESCALATED"));
    }

    /**
     * Submit moderation decision.
     */
    private void submitDecision(String decisionType) {
        if (selectedPost == null) {
            showAlert("Error", "No post selected");
            return;
        }

        String reason = decisionReasonArea.getText();
        
        try {
            Optional<ModerationDecisionDTO> response = apiService.submitDecision(
                selectedPost.getId().toString(),
                decisionType,
                reason
            );

            if (response.isPresent()) {
                log.info("Decision {} submitted for post {}", decisionType, selectedPost.getId());
                showAlert("Success", "Decision submitted: " + decisionType);
                decisionReasonArea.clear();
                flaggedPostsTable.getSelectionModel().clearSelection();
                loadModerationQueue(); // Refresh list
            } else {
                showAlert("Error", "Failed to submit decision");
            }
        } catch (ApiException e) {
            if (e.isUnauthorized() || e.isBanned()) {
                log.error("Unauthorized to submit decision: {}", e.getErrorMessage());
                showAlert("Access Denied", e.getErrorMessage());
            } else {
                showAlert("Error", e.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error submitting decision", e);
            showAlert("Error", "Error submitting decision: " + e.getMessage());
        }
    }

    /**
     * Load flagged posts from moderation queue.
     */
    private void loadModerationQueue() {
        try {
            Optional<RestApiClient.PagedResponse<?>> response = apiService.getModerationQueue(currentPage, pageSize);
            
            if (response.isPresent()) {
                flaggedPostsTable.getItems().clear();
                RestApiClient.PagedResponse<?> pagedResponse = response.get();
                
                if (pagedResponse.getContent() != null) {
                    int count = 0;
                    for (Object item : pagedResponse.getContent()) {
                        if (item instanceof PostDTO) {
                            flaggedPostsTable.getItems().add((PostDTO) item);
                            count++;
                        }
                    }
                    
                    postCountLabel.setText("Posts: " + count + " / " + pagedResponse.getTotalElements());
                    log.info("Loaded {} flagged posts from page {}", count, currentPage);
                }
            } else {
                showAlert("Error", "Failed to load moderation queue");
            }
        } catch (Exception e) {
            log.error("Error loading moderation queue", e);
            showAlert("Error", "Error loading moderation queue: " + e.getMessage());
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
