package core.shared.controllers;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class StatusIconController {

    public enum Status { OFFLINE, ONLINE, DISABLED }

    @FXML private ImageView icon;
    @FXML private Circle statusCircle;

    public void setIcon(Image image) {
        icon.setImage(image);
    }

    public void setStatusOnline(Status status) {
        statusCircle.setStroke(
                switch(status) {
                    case OFFLINE -> Color.GRAY;
                    case ONLINE -> Color.GREEN;
                    case DISABLED -> Color.TRANSPARENT;
                }
        );
    }
}
