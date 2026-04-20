package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.AppealDTO;
import com.example.socialapp.frontend.model.UserDTO;
import com.example.socialapp.frontend.service.RestApiClient;
import com.example.socialapp.frontend.util.ApiException;
import com.example.socialapp.frontend.util.RoleManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Admin Panel Controller.
 * Manages users, bans, and appeals.
 * Only accessible to users with ADMIN role.
 */
@Slf4j
public class AdminPanelController extends BaseController implements Initializable {
    // User Management Tab
    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, String> userIdColumn;
    @FXML private TableColumn<UserDTO, String> emailColumn;
    @FXML private TableColumn<UserDTO, String> nameColumn;
    @FXML private TableColumn<UserDTO, String> rolesColumn;
    @FXML private TableColumn<UserDTO, String> enabledColumn;
    @FXML private Button banUserButton;
    @FXML private Button unbanUserButton;
    @FXML private TextArea userActionReasonArea;
    @FXML private Spinner<Integer> usersPageSpinner;

    // Appeals Tab
    @FXML private TableView<AppealDTO> appealsTable;
    @FXML private TableColumn<AppealDTO, String> appealIdColumn;
    @FXML private TableColumn<AppealDTO, String> reasonColumn;
    @FXML private TableColumn<AppealDTO, String> statusColumn;
    @FXML private TableColumn<AppealDTO, String> createdAtColumn;
    @FXML private Button approveAppealButton;
    @FXML private Button rejectAppealButton;
    @FXML private TextArea appealDecisionArea;
    @FXML private Spinner<Integer> appealsPageSpinner;

    @FXML private Label titleLabel;
    @FXML private Button backButton;
    @FXML private Button viewLogsButton;
    @FXML private TabPane adminTabPane;

    private int usersPage = 0;
    private int appealsPage = 0;
    private int pageSize = 20;
    private UserDTO selectedUser = null;
    private AppealDTO selectedAppeal = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check user has ADMIN role
        if (!isUserAdmin()) {
            showAlert("Access Denied", "You do not have permission to access the admin panel.");
            navigateBack();
            return;
        }

        setupUI();
        setupUserManagementTab();
        setupAppealReviewTab();
        setupLogsButton();
        setupBackButton();
        loadUsers();
        loadAppeals();
    }

    /**
     * Check if current user is an admin.
     */
    private boolean isUserAdmin() {
        Optional<UserDTO> user = sessionManager.getCurrentUser();
        boolean isAdmin = RoleManager.isAdmin(user);
        
        if (!isAdmin) {
            log.warn("User does not have ADMIN role. Current user: {}, Roles: {}", 
                user.map(UserDTO::getEmail).orElse("Unknown"), 
                user.map(UserDTO::getRoles).orElse(java.util.Set.of()));
        }
        
        return isAdmin;
    }

    /**
     * Setup UI labels.
     */
    private void setupUI() {
        sessionManager.getCurrentUser().ifPresent(user -> {
            titleLabel.setText("Admin Panel - " + user.getFirstName());
        });
    }

    // ===== User Management Tab =====

    /**
     * Setup user management tab.
     */
    private void setupUserManagementTab() {
        setupUserTableColumns();
        setupUserTableSelection();
        setupUserActionButtons();
        setupUsersPageination();
    }

    /**
     * Setup user table columns.
     */
    private void setupUserTableColumns() {
        userIdColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId().toString()));
        
        emailColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        
        nameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));
        
        rolesColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getRoles() != null ? cellData.getValue().getRoles().toString() : ""));
        
        enabledColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getEnabled() != null && cellData.getValue().getEnabled() ? "Yes" : "No"));

        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Setup user table row selection.
     */
    private void setupUserTableSelection() {
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedUser = newVal;
            if (newVal != null) {
                boolean isEnabled = newVal.getEnabled() != null && newVal.getEnabled();
                banUserButton.setDisable(isEnabled);
                unbanUserButton.setDisable(!isEnabled);
                userActionReasonArea.setDisable(false);
            } else {
                banUserButton.setDisable(true);
                unbanUserButton.setDisable(true);
                userActionReasonArea.clear();
                userActionReasonArea.setDisable(true);
            }
        });
    }

    /**
     * Setup user action buttons.
     */
    private void setupUserActionButtons() {
        banUserButton.setDisable(true);
        unbanUserButton.setDisable(true);

        banUserButton.setOnAction(event -> banUser());
        unbanUserButton.setOnAction(event -> unbanUser());
    }

    /**
     * Setup users pagination.
     */
    private void setupUsersPageination() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0);
        usersPageSpinner.setValueFactory(valueFactory);
        usersPageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            usersPage = newVal;
            loadUsers();
        });
    }

    /**
     * Ban a user.
     */
    private void banUser() {
        if (selectedUser == null) {
            showAlert("Error", "No user selected");
            return;
        }

        String reason = userActionReasonArea.getText();
        if (reason.isEmpty()) {
            showAlert("Validation Error", "Please provide a reason for banning");
            return;
        }

        try {
            boolean success = apiService.banUser(selectedUser.getId().toString(), reason);
            if (success) {
                log.info("User {} banned", selectedUser.getEmail());
                showAlert("Success", "User has been banned");
                userActionReasonArea.clear();
                usersTable.getSelectionModel().clearSelection();
                loadUsers();
            } else {
                showAlert("Error", "Failed to ban user");
            }
        } catch (ApiException e) {
            log.error("Error banning user: {}", e.getErrorMessage());
            showAlert("Error", e.getErrorMessage());
        }
    }

    /**
     * Unban a user.
     */
    private void unbanUser() {
        if (selectedUser == null) {
            showAlert("Error", "No user selected");
            return;
        }

        try {
            boolean success = apiService.unbanUser(selectedUser.getId().toString());
            if (success) {
                log.info("User {} unbanned", selectedUser.getEmail());
                showAlert("Success", "User has been unbanned");
                userActionReasonArea.clear();
                usersTable.getSelectionModel().clearSelection();
                loadUsers();
            } else {
                showAlert("Error", "Failed to unban user");
            }
        } catch (ApiException e) {
            log.error("Error unbanning user: {}", e.getErrorMessage());
            showAlert("Error", e.getErrorMessage());
        }
    }

    /**
     * Load users from backend.
     */
    private void loadUsers() {
        try {
            Optional<RestApiClient.PagedResponse<?>> response = apiService.getUsers(usersPage, pageSize);
            
            if (response.isPresent()) {
                usersTable.getItems().clear();
                RestApiClient.PagedResponse<?> pagedResponse = response.get();
                
                if (pagedResponse.getContent() != null) {
                    for (Object item : pagedResponse.getContent()) {
                        if (item instanceof UserDTO) {
                            usersTable.getItems().add((UserDTO) item);
                        }
                    }
                    
                    log.info("Loaded {} users from page {}", usersTable.getItems().size(), usersPage);
                }
            } else {
                showAlert("Error", "Failed to load users");
            }
        } catch (Exception e) {
            log.error("Error loading users", e);
            showAlert("Error", "Error loading users: " + e.getMessage());
        }
    }

    // ===== Appeals Review Tab =====

    /**
     * Setup appeals review tab.
     */
    private void setupAppealReviewTab() {
        setupAppealTableColumns();
        setupAppealTableSelection();
        setupAppealDecisionButtons();
        setupAppealsPageination();
    }

    /**
     * Setup appeals table columns.
     */
    private void setupAppealTableColumns() {
        appealIdColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId().toString()));
        
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
     * Setup appeals table row selection.
     */
    private void setupAppealTableSelection() {
        appealsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedAppeal = newVal;
            if (newVal != null) {
                boolean isPending = "PENDING".equals(newVal.getStatus());
                approveAppealButton.setDisable(!isPending);
                rejectAppealButton.setDisable(!isPending);
                appealDecisionArea.setDisable(!isPending);
            } else {
                approveAppealButton.setDisable(true);
                rejectAppealButton.setDisable(true);
                appealDecisionArea.clear();
                appealDecisionArea.setDisable(true);
            }
        });
    }

    /**
     * Setup appeal decision buttons.
     */
    private void setupAppealDecisionButtons() {
        approveAppealButton.setDisable(true);
        rejectAppealButton.setDisable(true);

        approveAppealButton.setOnAction(event -> decideAppeal("APPROVED"));
        rejectAppealButton.setOnAction(event -> decideAppeal("REJECTED"));
    }

    /**
     * Setup appeals pagination.
     */
    private void setupAppealsPageination() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0);
        appealsPageSpinner.setValueFactory(valueFactory);
        appealsPageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            appealsPage = newVal;
            loadAppeals();
        });
    }

    /**
     * Make appeal decision.
     */
    private void decideAppeal(String decision) {
        if (selectedAppeal == null) {
            showAlert("Error", "No appeal selected");
            return;
        }

        String adminDecision = appealDecisionArea.getText();
        if (adminDecision.isEmpty()) {
            showAlert("Validation Error", "Please provide a decision reason");
            return;
        }

        try {
            boolean success = apiService.decideAppeal(
                selectedAppeal.getId().toString(),
                decision,
                adminDecision
            );

            if (success) {
                log.info("Appeal {} decision: {}", selectedAppeal.getId(), decision);
                showAlert("Success", "Appeal decision submitted: " + decision);
                appealDecisionArea.clear();
                appealsTable.getSelectionModel().clearSelection();
                loadAppeals();
            } else {
                showAlert("Error", "Failed to submit appeal decision");
            }
        } catch (ApiException e) {
            log.error("Error deciding appeal: {}", e.getErrorMessage());
            showAlert("Error", e.getErrorMessage());
        }
    }

    /**
     * Load appeals from backend.
     */
    private void loadAppeals() {
        try {
            Optional<RestApiClient.PagedResponse<?>> response = apiService.getAppeals(appealsPage, pageSize);
            
            if (response.isPresent()) {
                appealsTable.getItems().clear();
                RestApiClient.PagedResponse<?> pagedResponse = response.get();
                
                if (pagedResponse.getContent() != null) {
                    for (Object item : pagedResponse.getContent()) {
                        if (item instanceof AppealDTO) {
                            appealsTable.getItems().add((AppealDTO) item);
                        }
                    }
                    
                    log.info("Loaded {} appeals from page {}", appealsTable.getItems().size(), appealsPage);
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
     * Setup back button.
     */
    private void setupBackButton() {
        backButton.setOnAction(event -> navigateBack());
    }

    /**
     * Setup logs button.
     */
    private void setupLogsButton() {
        viewLogsButton.setOnAction(event -> navigateToLogs());
    }

    /**
     * Navigate to moderation logs view.
     */
    private void navigateToLogs() {
        try {
            Stage stage = (Stage) viewLogsButton.getScene().getWindow();
            replaceScene("moderation-logs.fxml", "Moderation Logs", stage);
        } catch (Exception e) {
            log.error("Error navigating to moderation logs", e);
            showAlert("Error", "Failed to navigate to moderation logs");
        }
    }

    /**
     * Navigate back to feed.
     */
    private void navigateBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            replaceScene("dashboard.fxml", "Social App - Feed", stage);
        } catch (Exception e) {
            log.error("Error navigating back", e);
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
