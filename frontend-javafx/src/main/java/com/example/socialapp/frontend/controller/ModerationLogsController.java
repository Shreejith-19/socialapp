package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.ModerationLogDTO;
import com.example.socialapp.frontend.service.ModerationLogService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Moderation Logs Controller.
 * Displays moderation action logs in a table.
 * Only accessible to ADMIN users.
 */
@Slf4j
public class ModerationLogsController extends BaseController implements Initializable {
    @FXML private Label pageTitle;
    @FXML private TableView<ModerationLogDTO> logsTable;
    @FXML private TableColumn<ModerationLogDTO, String> moderatorNameColumn;
    @FXML private TableColumn<ModerationLogDTO, UUID> moderatorIdColumn;
    @FXML private TableColumn<ModerationLogDTO, UUID> postIdColumn;
    @FXML private TableColumn<ModerationLogDTO, String> actionColumn;
    @FXML private TableColumn<ModerationLogDTO, LocalDateTime> timestampColumn;
    @FXML private Button backButton;
    @FXML private Label emptyStateLabel;

    private ModerationLogService moderationLogService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        moderationLogService = ModerationLogService.getInstance();
        setupTableColumns();
        loadLogs();
        setupBackButton();
    }

    /**
     * Setup table columns.
     */
    private void setupTableColumns() {
        moderatorNameColumn.setCellValueFactory(new PropertyValueFactory<>("moderatorName"));
        moderatorIdColumn.setCellValueFactory(new PropertyValueFactory<>("moderatorId"));
        postIdColumn.setCellValueFactory(new PropertyValueFactory<>("postId"));
        actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Set column widths
        moderatorNameColumn.setPrefWidth(200);
        moderatorIdColumn.setPrefWidth(250);
        postIdColumn.setPrefWidth(250);
        actionColumn.setPrefWidth(150);
        timestampColumn.setPrefWidth(200);

        // Setup action column with styling
        actionColumn.setCellFactory(col -> new javafx.scene.control.TableCell<ModerationLogDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText(item);
                    if ("APPROVED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else if ("REMOVED".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    /**
     * Load moderation logs from backend.
     */
    private void loadLogs() {
        // Load in background thread
        new Thread(() -> {
            var logsOptional = moderationLogService.getAllLogs();

            Platform.runLater(() -> {
                if (logsOptional.isPresent()) {
                    List<ModerationLogDTO> logs = logsOptional.get();
                    if (logs.isEmpty()) {
                        showEmptyState();
                    } else {
                        displayLogs(logs);
                    }
                } else {
                    showErrorState("Failed to load moderation logs");
                }
            });
        }).start();
    }

    /**
     * Display logs in table.
     */
    private void displayLogs(List<ModerationLogDTO> logs) {
        logsTable.setItems(FXCollections.observableArrayList(logs));
        logsTable.setVisible(true);
        emptyStateLabel.setVisible(false);
        pageTitle.setText("Moderation Logs (" + logs.size() + " entries)");
        log.info("Displayed {} moderation logs", logs.size());
    }

    /**
     * Show empty state.
     */
    private void showEmptyState() {
        logsTable.setVisible(false);
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setText("No moderation logs found");
        pageTitle.setText("Moderation Logs (0 entries)");
    }

    /**
     * Show error state.
     */
    private void showErrorState(String message) {
        logsTable.setVisible(false);
        emptyStateLabel.setVisible(true);
        emptyStateLabel.setText(message);
        log.warn("Error loading logs: {}", message);
    }

    /**
     * Setup back button.
     */
    private void setupBackButton() {
        backButton.setOnAction(event -> {
            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) backButton.getScene().getWindow();
                replaceScene("admin.fxml", "Admin Panel", stage);
            } catch (Exception e) {
                log.error("Error navigating back", e);
            }
        });
    }
}
