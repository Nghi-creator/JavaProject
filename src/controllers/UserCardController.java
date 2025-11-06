package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class UserCardController {

    @FXML private ImageView avatarImage;
    @FXML private Circle statusCircle;
    @FXML private Label usernameLabel;

    public void setUser(String username, String imagePath) {
        usernameLabel.setText(username);
        avatarImage.setImage(new Image(getClass().getResourceAsStream(imagePath)));
    }

    public void setStatusOnline(boolean online) {
        if (online) {
            statusCircle.setStroke(Color.web("#57f287")); // green
        } else {
            statusCircle.setStroke(Color.GRAY); // offline
        }
    }
}
