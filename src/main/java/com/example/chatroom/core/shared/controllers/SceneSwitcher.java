package com.example.chatroom.core.shared.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class SceneSwitcher {

    // --- Stage Scene Switching ---
    public static <T> void switchScene(Stage stage, String fxmlPath, Consumer<T> controllerConsumer) {
        try {
            FXMLLoader loader = loadFXML(fxmlPath);
            Parent root = loader.load();

            T controller = loader.getController();
            if (controllerConsumer != null && controller != null) {
                controllerConsumer.accept(controller);
            }

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

    // Overload without Consumer
    public static void switchScene(Stage stage, String fxmlPath) {
        switchScene(stage, fxmlPath, null);
    }

    public static <T> void switchScene(Node sourceNode, String fxmlPath, Consumer<T> controllerConsumer) {
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        switchScene(stage, fxmlPath, controllerConsumer);
    }

    public static void switchScene(Node sourceNode, String fxmlPath) {
        switchScene(sourceNode, fxmlPath, null);
    }

    // --- Popups ---
    public static <T> void openPopup(String fxmlPath, String title, Consumer<T> controllerConsumer) {
        try {
            FXMLLoader loader = loadFXML(fxmlPath);
            Parent root = loader.load();

            T controller = loader.getController();
            if (controllerConsumer != null && controller != null) {
                controllerConsumer.accept(controller);
            }

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

    public static void openPopup(String fxmlPath, String title) {
        openPopup(fxmlPath, title, null);
    }

    public static void showMessage(String msg) {
        openPopup("/shared/ui/fxml/MessagePopup.fxml", "Message", (MessagePopupController controller) -> {
            controller.setMessage(msg);
        });
    }

    // --- Helper to load FXML ---
    private static FXMLLoader loadFXML(String fxmlPath) throws IOException {
        URL fxmlUrl = SceneSwitcher.class.getResource(fxmlPath);
        if (fxmlUrl == null) {
            throw new IOException("Cannot find FXML file: " + fxmlPath);
        }
        return new FXMLLoader(fxmlUrl);
    }
}
