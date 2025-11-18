package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneSwitcher {

    /**
     * Helper method to switch scenes.
     *
     * @param event The ActionEvent from the button click.
     * @param fxmlPath The path to the new FXML file (e.g., "/fxml/NewScreen.fxml").
     */
    public static void switchScene(ActionEvent event, String fxmlPath) {
        try {
            // 1. Get the URL of the new FXML file
            URL fxmlUrl = SceneSwitcher.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file: " + fxmlPath);
                return;
            }

            // 2. Load the new FXML
            Parent root = FXMLLoader.load(fxmlUrl);

            // 3. Get the current stage (window)
            // We get the Node that triggered the event, get its Scene, then get its Window (Stage)
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 4. Set the new scene on the stage
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            stage.setScene(scene);

        } catch (IOException e) {
            System.err.println("Failed to switch scene to " + fxmlPath);
            e.printStackTrace();
        }
    }
}