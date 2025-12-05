package com.example.chatroom.admin;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

/**
 * Main Application Class for admins
 * This class loads the FXML file, sets up the scene,
 * and displays the main application window.
 */

public class AdminApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {

            URL fxmlUrl = getClass().getResource("/admin/ui/fxml/AdminUserView.fxml");
            
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file.");
                return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root, 1280, 720);
            

            URL cssUrl = getClass().getResource("/shared/ui/css/DiscordTheme.css");
            if (cssUrl != null) {
               scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setTitle("Admin Dashboard");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}