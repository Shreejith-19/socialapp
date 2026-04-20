package com.example.socialapp.frontend.controller;

import com.example.socialapp.frontend.model.NotificationDTO;
import com.example.socialapp.frontend.service.NotificationService;
import com.example.socialapp.frontend.service.RestApiClient;
import com.example.socialapp.frontend.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Notification Controller.
 * Displays user notifications with pagination and sorting by latest.
 * Follows MVC pattern with NotificationService for API calls.
 */
@Slf4j
public class NotificationController extends BaseController implements Initializable {
    
    @FXML private Label pageTitle;
    @FXML private VBox notificationsList;
    @FXML private ScrollPane notificationsScroll;
    @FXML private Button backButton;
    @FXML private Button markAllAsReadButton;
    @FXML private Spinner<Integer> pageSpinner;
    @FXML private Label emptyLabel;
    
    private int currentPage = 0;
    private int pageSize = 15;
    private NotificationService notificationService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Initializing NotificationController");
        
        // Get NotificationService singleton instance
        notificationService = NotificationService.getInstance();
        
        setupUI();
        setupPagination();
        setupButtons();
        loadNotifications();
    }
    
    /**
     * Setup UI components.
     */
    private void setupUI() {
        pageTitle.setText("My Notifications");
        notificationsList.setSpacing(10);
        notificationsScroll.setFitToWidth(true);
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
            loadNotifications();
        });
    }
    
    /**
     * Setup buttons.
     */
    private void setupButtons() {
        backButton.setOnAction(event -> handleBack());
        markAllAsReadButton.setOnAction(event -> handleMarkAllAsRead());
    }
    
    /**
     * Load notifications from backend.
     */
    private void loadNotifications() {
        log.info("Loading notifications - page: {}, size: {}", currentPage, pageSize);
        
        new Thread(() -> {
            try {
                Optional<RestApiClient.PagedResponse<NotificationDTO>> response = 
                    notificationService.getAllNotifications(currentPage, pageSize);
                
                Platform.runLater(() -> {
                    if (response.isPresent()) {
                        displayNotifications(response.get());
                    } else {
                        showEmptyState();
                    }
                });
            } catch (Exception e) {
                log.error("Error loading notifications", e);
                Platform.runLater(this::showEmptyState);
            }
        }).start();
    }
    
    /**
     * Display notifications in the list.
     */
    private void displayNotifications(RestApiClient.PagedResponse<NotificationDTO> response) {
        notificationsList.getChildren().clear();
        
        if (response.getContent() == null || response.getContent().isEmpty()) {
            showEmptyState();
            return;
        }
        
        for (NotificationDTO notification : response.getContent()) {
            VBox notificationCard = createNotificationCard(notification);
            notificationsList.getChildren().add(notificationCard);
        }
        
        // Update pagination
        int maxPage = response.getTotalPages() - 1;
        if (pageSpinner.getValue() > maxPage) {
            pageSpinner.getValueFactory().setValue(0);
        }
        
        log.info("Displayed {} notifications", response.getContent().size());
    }
    
    /**
     * Create a notification card UI element.
     */
    private VBox createNotificationCard(NotificationDTO notification) {
        VBox card = new VBox();
        card.setStyle("-fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-background-color: " +
                      (notification.isRead() ? "#f5f5f5" : "#ffffff") + ";");
        card.setSpacing(8);
        
        // Header: Type badge + Time + Delete button
        HBox header = new HBox();
        header.setSpacing(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label typeBadge = new Label(notification.getType() != null ? notification.getType() : "NOTIFICATION");
        typeBadge.getStyleClass().addAll("badge", notification.getTypeBadgeClass());
        typeBadge.setPrefWidth(100);
        
        Label timeLabel = new Label(notification.getFormattedTime());
        timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6;");
        
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle("-fx-padding: 2 6; -fx-font-size: 10; -fx-background-color: #e74c3c; -fx-text-fill: white;");
        deleteBtn.setOnAction(event -> handleDeleteNotification(notification));
        
        header.getChildren().addAll(typeBadge, timeLabel, spacer, deleteBtn);
        
        // Message
        Label message = new Label(notification.getMessage() != null ? notification.getMessage() : "");
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50;");
        
        // Footer: Mark as read button (if unread)
        if (!notification.isRead()) {
            HBox footer = new HBox();
            footer.setAlignment(Pos.CENTER_RIGHT);
            
            Button markReadBtn = new Button("Mark as Read");
            markReadBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 10; -fx-background-color: #3498db; -fx-text-fill: white;");
            markReadBtn.setOnAction(event -> handleMarkAsRead(notification));
            
            footer.getChildren().add(markReadBtn);
            card.getChildren().addAll(header, message, footer);
        } else {
            card.getChildren().addAll(header, message);
        }
        
        return card;
    }
    
    /**
     * Show empty state when no notifications.
     */
    private void showEmptyState() {
        notificationsList.getChildren().clear();
        
        VBox emptyBox = new VBox();
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 60; -fx-spacing: 10;");
        
        Label emptyIcon = new Label("🔔");
        emptyIcon.setStyle("-fx-font-size: 48;");
        
        Label emptyTitle = new Label("No Notifications");
        emptyTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        Label emptyDesc = new Label("You're all caught up!");
        emptyDesc.setStyle("-fx-font-size: 12; -fx-text-fill: #95a5a6;");
        
        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);
        notificationsList.getChildren().add(emptyBox);
    }
    
    /**
     * Handle mark as read.
     */
    private void handleMarkAsRead(NotificationDTO notification) {
        log.info("Marking notification {} as read", notification.getId());
        
        new Thread(() -> {
            notificationService.markAsRead(notification.getId());
            Platform.runLater(this::loadNotifications);
        }).start();
    }
    
    /**
     * Handle mark all as read.
     */
    private void handleMarkAllAsRead() {
        log.info("Marking all notifications as read");
        
        new Thread(() -> {
            boolean success = notificationService.markAllAsRead();
            Platform.runLater(() -> {
                if (success) {
                    AlertUtils.showInfo("Success", "All notifications marked as read");
                    loadNotifications();
                } else {
                    AlertUtils.showError("Error", "Failed to mark notifications as read");
                }
            });
        }).start();
    }
    
    /**
     * Handle delete notification.
     */
    private void handleDeleteNotification(NotificationDTO notification) {
        log.info("Deleting notification {}", notification.getId());
        
        new Thread(() -> {
            boolean success = notificationService.deleteNotification(notification.getId());
            Platform.runLater(() -> {
                if (success) {
                    loadNotifications();
                } else {
                    AlertUtils.showError("Error", "Failed to delete notification");
                }
            });
        }).start();
    }
    
    /**
     * Handle back button.
     */
    private void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            replaceScene("dashboard.fxml", "Social App - Dashboard", stage);
        } catch (Exception e) {
            log.error("Error navigating back to dashboard", e);
            AlertUtils.showError("Error", "Could not navigate back");
        }
    }
}
