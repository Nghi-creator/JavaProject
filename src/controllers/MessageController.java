package controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class MessageController {

    @FXML private StackPane statusIcon;
    @FXML private Text title;
    @FXML private Text timestamp;
    @FXML private Text content;

    @FXML private StatusIconController statusIconController;

    @FXML
    private void initialize() {

        Object controllerObj = statusIcon.getProperties().get("fx:controller");
        if (controllerObj instanceof StatusIconController controller) {
            statusIconController = controller;
            statusIconController.setStatusOnline(StatusIconController.Status.ONLINE);
        }
    }

    public void setStatus(StatusIconController.Status status) {
        if (statusIconController != null) {
            statusIconController.setStatusOnline(status);
        }
    }

    public void setIcon(Image image) {
        if (statusIconController != null) {
            statusIconController.setIcon(image);
        }
    }

    public void setTitle(String title) { this.title.setText(title); }
    public void setTimeStamp(String timestamp) { this.timestamp.setText(timestamp); }
    public void setContent(String content) { this.content.setText(content); }

}
