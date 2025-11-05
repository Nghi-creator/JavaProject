import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

/**
 * Main Application Class
 * This class loads the FXML file, sets up the scene,
 * and displays the main application window.
 */
public class ChatApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Load the FXML file
            // We use getResource to find the file in our project structure

            // **** THIS IS THE CORRECTED LINE ****
            // We need to tell it to look inside the "src" folder
            URL fxmlUrl = new File("src/ChatroomView.fxml").toURI().toURL();

            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file. Make sure 'ChatroomView.fxml' is in the 'src' directory.");
                return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);

            // 2. Create the Scene
            Scene scene = new Scene(root, 1280, 720);

            // 3. Load the CSS file
            // We also need to fix the path for the CSS file.
            // The FXML file links to "@DiscordTheme.css", but it's also in the "src" folder.
            // Let's add it from the code to be safe.
            URL cssUrl = new File("src/DiscordTheme.css").toURI().toURL();
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("Cannot find CSS file. Make sure 'DiscordTheme.css' is in the 'src' directory.");
            }


            // 4. Configure and show the Stage (the main window)
            primaryStage.setTitle("Discord Clone - Chatroom");
            primaryStage.setScene(scene);
            primaryStage.setMinHeight(600);
            primaryStage.setMinWidth(1000);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Launch the JavaFX application
        launch(args);
    }
}