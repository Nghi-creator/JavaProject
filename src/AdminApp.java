import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class AdminApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the default view: User Management
            URL fxmlUrl = getClass().getResource("/fxml/AdminUserView.fxml");
            
            if (fxmlUrl == null) {
                System.err.println("Cannot find FXML file.");
                return;
            }
            Parent root = FXMLLoader.load(fxmlUrl);
            Scene scene = new Scene(root, 1280, 720);
            
            // Load the shared CSS
            URL cssUrl = getClass().getResource("/css/DiscordTheme.css");
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