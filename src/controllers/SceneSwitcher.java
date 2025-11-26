package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneSwitcher {

    /**
     * Switch scene using a stage directly.
     * @param stage The stage to replace the scene in.
     * @param fxmlPath Path to FXML resource.
     */
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

        } catch (IOException e) {
            System.err.println("Failed to switch scene to " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void switchScene(javafx.scene.Node sourceNode, String fxmlPath) {
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        switchScene(stage, fxmlPath);
    }
}
