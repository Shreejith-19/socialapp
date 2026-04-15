package com.example.socialapp.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Social App JavaFX Application.
 * Main entry point for the desktop client.
 */
@Slf4j
public class SocialAppApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            log.info("Starting Social App JavaFX Application");
            
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/socialapp/frontend/view/login.fxml")
            );
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            primaryStage.setTitle("Social App - Login");
            primaryStage.setScene(scene);
            primaryStage.setWidth(500);
            primaryStage.setHeight(400);
            primaryStage.show();
            
            log.info("Application started successfully");
        } catch (Exception e) {
            log.error("Error starting application", e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
