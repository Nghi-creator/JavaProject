package com.example.chatroom.core.shared.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MessagePopupController {

    @FXML
    private Label messageLabel;

    private Stage stage;

    /** Set the stage so we can close it later */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /** Set the message to display */
    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    @FXML
    private void handleClose() {
        if (stage != null) {
            stage.close();
        }
    }
}
