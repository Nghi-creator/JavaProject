package com.example.core.shared.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;

public class NameCardController {

    @FXML private StackPane statusIcon;
    @FXML private Label nameLabel;

    private StatusIconController statusIconController;

    @FXML
    private void initialize() {

        Object controllerObj = statusIcon.getProperties().get("fx:controller");
        if (controllerObj instanceof StatusIconController controller) {
            statusIconController = controller;
            statusIconController.setStatusOnline(StatusIconController.Status.ONLINE);
        }
    }

    public void setName(String name) {
        nameLabel.setText(name);
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
}
