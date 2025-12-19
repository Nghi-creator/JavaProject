package com.example.chatroom.core.shared.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class NameCardController {

    @FXML private StackPane statusIcon;
    @FXML private Label nameLabel;

    @FXML private StatusIconController statusIconController;

    private String username;

    @FXML
    private void initialize() {
        FXMLLoader loader = (FXMLLoader) statusIcon.getProperties().get("javafx.fxml.FXMLLoader");
        if (loader != null) {
            statusIconController = loader.getController();
        }

        statusIcon.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                showContextMenu(event.getScreenX(), event.getScreenY());
            }
        });
    }

    private void showContextMenu(double x, double y) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("transparent-context-menu");

        javafx.scene.control.Button reportBtn = new javafx.scene.control.Button("Report " + (username != null ? username : ""));
        reportBtn.getStyleClass().add("report-context-button");

        // Handle click
        reportBtn.setOnAction(e -> {
            contextMenu.hide();
            openReportWindow();
        });

        javafx.scene.control.CustomMenuItem item = new javafx.scene.control.CustomMenuItem(reportBtn);
        item.setHideOnClick(true);
        item.getStyleClass().add("transparent-menu-item");

        contextMenu.getItems().add(item);

        contextMenu.setOnShown(e -> {
            javafx.scene.Scene scene = contextMenu.getScene();
            if (scene != null && scene.getRoot() != null) {
                scene.getRoot().setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-padding: 0;" +
                                "-fx-effect: null;"
                );
            }
        });

        contextMenu.show(statusIcon, x, y);
    }

    private void openReportWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/ui/fxml/ReportUserView.fxml"));
            Parent root = loader.load();

            com.example.chatroom.user.controllers.ReportUserViewController controller = loader.getController();
            controller.setReportedUser(this.username);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Report User");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(String username, String fullName) {
        this.username = username;
        if (fullName != null && !fullName.isEmpty()) {
            nameLabel.setText(fullName + " (@" + username + ")");
        } else {
            nameLabel.setText(username);
        }
    }

    public void setName(String name) { nameLabel.setText(name); }
    public void setStatus(StatusIconController.Status status) { if (statusIconController != null) statusIconController.setStatus(status); }
    public void setIcon(Image image) { if (statusIconController != null) statusIconController.setIcon(image); }
}