package com.example.chatroom.core.shared.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneSwitcher {

    public static void switchTo(Class<?> originClass, ActionEvent event, String fxmlPath) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            switchScene(stage, fxmlPath);
        } catch (Exception e) {
            System.err.println("Failed to switch to: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void switchScene(Stage stage, String fxmlPath) {
        try {
            URL fxmlUrl = SceneSwitcher.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            stage.show();

        } catch (IOException e) {
            System.err.println("Failed to switch scene to " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void switchScene(Node sourceNode, String fxmlPath) {
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        switchScene(stage, fxmlPath);
    }

    // --- 3. Popup Logic ---
    public static void openPopup(String fxmlPath, String title) {
        try {
            URL fxmlUrl = SceneSwitcher.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Cannot find Popup FXML: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            System.err.println("Failed to open popup: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void showMessage(String msg) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource("/shared/ui/fxml/MessagePopup.fxml"));
            Parent root = loader.load();

            MessagePopupController controller = loader.getController();
            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setMessage(msg);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Message");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}