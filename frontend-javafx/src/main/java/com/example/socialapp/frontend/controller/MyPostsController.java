package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.PostDTO;
import com.example.socialapp.frontend.service.RestApiClient;
import com.example.socialapp.frontend.util.AlertUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for displaying user's posts with their moderation status.
 * Shows all posts (PUBLISHED, FLAGGED, REMOVED) created by the authenticated user.
 */
@Slf4j
public class MyPostsController extends BaseController implements Initializable {
    @FXML private TableView<PostDTO> myPostsTable;
    @FXML private TableColumn<PostDTO, String> contentColumn;
    @FXML private TableColumn<PostDTO, String> statusColumn;
    @FXML private TableColumn<PostDTO, String> createdAtColumn;
    @FXML private TableColumn<PostDTO, Void> actionsColumn;
    @FXML private Button backButton;
    @FXML private Button refreshButton;
    @FXML private Spinner<Integer> pageSpinner;

    private int currentPage = 0;
    private int pageSize = 10;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupActionsColumn();
        setupPagination();
        setupBackButton();
        setupRefreshButton();
        loadMyPosts();
    }

    /**
     * Setup table columns with proper cell value factories.
     */
    private void setupTableColumns() {
        contentColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSummary()));
        
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatus() != null ? 
                cellData.getValue().getStatus().toString() : "UNKNOWN"));
        
        createdAtColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null ? 
                cellData.getValue().getCreatedAt().toLocalDate().toString() : "N/A"));

        myPostsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /**
     * Setup actions column with custom cell rendering.
     * Shows "Appeal" button only for REMOVED posts.
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<PostDTO, Void>() {
            private final Button appealButton = new Button("Appeal");
            private final HBox cellContent = new HBox();

            {
                appealButton.setStyle("-fx-font-size: 11; -fx-padding: 5 10;");
                appealButton.setOnAction(event -> {
                    PostDTO post = getTableView().getItems().get(getIndex());
                    openPostAppealDialog(post);
                });
                
                cellContent.setAlignment(Pos.CENTER);
                cellContent.setSpacing(5);
                cellContent.getChildren().add(appealButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    PostDTO post = getTableView().getItems().get(getIndex());
                    
                    // Show button only if status is REMOVED
                    if ("REMOVED".equals(post.getStatus())) {
                        appealButton.setVisible(true);
                        appealButton.setManaged(true);
                        setGraphic(cellContent);
                    } else {
                        // Hide button for non-REMOVED posts
                        appealButton.setVisible(false);
                        appealButton.setManaged(false);
                        setGraphic(null);
                    }
                }
            }
        });
    }

    /**
     * Setup pagination spinner.
     */
    private void setupPagination() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0);
        pageSpinner.setValueFactory(valueFactory);
        pageSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPage = newVal;
            loadMyPosts();
        });
    }

    /**
     * Setup back button to return to feed.
     */
    private void setupBackButton() {
        backButton.setOnAction(event -> goBackToFeed());
    }

    /**
     * Setup refresh button.
     */
    private void setupRefreshButton() {
        refreshButton.setOnAction(event -> loadMyPosts());
    }

    /**
     * Open appeal dialog for a removed post.
     * Shows a dialog where user can enter appeal reason.
     */
    private void openPostAppealDialog(PostDTO post) {
        try {
            log.info("Opening appeal dialog for post: {}", post.getId());
            
            // Create dialog
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Appeal Post Removal");
            dialog.setHeaderText("Appeal the removal of your post");
            dialog.setResizable(true);
            
            // Create content
            VBox content = new VBox();
            content.setSpacing(10);
            content.setPrefWidth(400);
            content.setStyle("-fx-padding: 15;");
            
            Label postPreview = new Label("Post: " + post.getSummary());
            postPreview.setStyle("-fx-font-weight: bold;");
            postPreview.setWrapText(true);
            
            Label reasonLabel = new Label("Explain why this post should be restored:");
            TextArea reasonTextArea = new TextArea();
            reasonTextArea.setPrefRowCount(6);
            reasonTextArea.setWrapText(true);
            reasonTextArea.setStyle("-fx-control-inner-background: white; -fx-padding: 5;");
            
            content.getChildren().addAll(postPreview, reasonLabel, reasonTextArea);
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            // Set result converter to extract the text from TextArea
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return reasonTextArea.getText();
                }
                return null;
            });
            
            // Handle submit
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(reason -> {
                if (reason != null && !reason.trim().isEmpty()) {
                    submitPostAppeal(post, reason);
                } else {
                    showAlert("Validation Error", "Please provide a reason for your appeal");
                }
            });
        } catch (Exception e) {
            log.error("Error opening appeal dialog", e);
            showAlert("Error", "Could not open appeal dialog: " + e.getMessage());
        }
    }

    /**
     * Submit a post appeal through the API.
     */
    private void submitPostAppeal(PostDTO post, String reason) {
        try {
            // Validation
            if (reason == null || reason.trim().isEmpty()) {
                showAlert("Validation Error", "Please provide a reason for your appeal");
                return;
            }
            
            log.info("Submitting post appeal for post: {}", post.getId());
            
            // Call API to submit post appeal
            Optional<Object> response = apiService.submitPostAppeal(post.getId().toString(), reason);
            
            if (response.isPresent()) {
                log.info("Post appeal submitted successfully");
                showAlert("Success", "Your appeal has been submitted successfully!");
                loadMyPosts(); // Refresh the table
            } else {
                showAlert("Error", "Failed to submit appeal. Please try again.");
            }
        } catch (Exception e) {
            log.error("Error submitting post appeal", e);
            showAlert("Error", "Could not submit appeal: " + e.getMessage());
        }
    }

    /**
     * Load user's posts from the API.
     */
    private void loadMyPosts() {
        try {
            log.info("Loading user's posts from /posts/my endpoint");
            
            Optional<RestApiClient.PagedResponse<?>> response = apiService.getMyPosts(currentPage, pageSize);
            
            if (response.isPresent()) {
                myPostsTable.getItems().clear();
                RestApiClient.PagedResponse<?> pagedResponse = response.get();
                
                if (pagedResponse.getContent() != null) {
                    for (Object item : pagedResponse.getContent()) {
                        if (item instanceof PostDTO) {
                            myPostsTable.getItems().add((PostDTO) item);
                        }
                    }
                }
                
                log.info("Loaded {} posts from page {}", myPostsTable.getItems().size(), currentPage);
            } else {
                showAlert("Info", "No posts found");
            }
        } catch (Exception e) {
            log.error("Error loading user's posts", e);
            handleException(e);
        }
    }

    /**
     * Go back to feed screen.
     */
    private void goBackToFeed() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            replaceScene("dashboard.fxml", "Social App - Feed", stage);
        } catch (Exception e) {
            log.error("Error navigating back to feed", e);
            showAlert("Error", "Could not navigate back to feed");
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
