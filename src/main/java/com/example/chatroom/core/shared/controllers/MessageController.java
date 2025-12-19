package com.example.chatroom.core.shared.controllers;

import com.example.chatroom.core.dto.MessageDto;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public class MessageController {

    @FXML private HBox root;
    @FXML private StackPane statusIcon;
    @FXML private Text title;
    @FXML private Text timestamp;
    @FXML private Text content;

    @FXML private StatusIconController statusIconController;

    private MessageDto message;
    private Runnable onDelete;


    @FXML
    private void initialize() {

        Object controllerObj = statusIcon.getProperties().get("fx:controller");
        if (controllerObj instanceof StatusIconController controller) {
            statusIconController = controller;
            statusIconController.setStatus(StatusIconController.Status.ONLINE);
        }
    }

    public void setStatus(StatusIconController.Status status) {
        if (statusIconController != null) {
            statusIconController.setStatus(status);
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

    public void setMessage(MessageDto message, Runnable onDelete) {
        this.message = message;
        this.onDelete = onDelete;

        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete message");

        deleteItem.setOnAction(e -> {
            if (onDelete != null) onDelete.run();
        });

        menu.getItems().add(deleteItem);

        // Attach to entire message bubble
        root.setOnContextMenuRequested(e -> {
            menu.show(root, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }



}
