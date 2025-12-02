package com.example.user;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

/**
 * Main Application Class for users
 * This class loads the FXML file, sets up the scene,
 * and displays the main application window.
 */

public class ChatApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            URL fxmlUrl = getClass().getResource("/user/ui/fxml/ChatroomView.fxml");

            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file. Make sure 'ChatroomView.fxml' is in the 'src' directory.");
                return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);

            Scene scene = new Scene(root, 1280, 720);

            URL cssUrl = getClass().getResource("/shared/ui/css/DiscordTheme.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Cannot find CSS file. Make sure 'DiscordTheme.css' is in the 'src' directory.");
            }

            primaryStage.setTitle("Chatroom");
            primaryStage.setScene(scene);
            primaryStage.setMinHeight(600);
            primaryStage.setMinWidth(1000);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}