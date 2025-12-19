package com.example.chatroom.core.shared.controllers;

import com.example.chatroom.core.dto.ConversationDto;
import com.example.chatroom.user.controllers.GroupSettingsViewController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class NameCardController {

    @FXML private StackPane statusIcon;
    @FXML private Label nameLabel;

    private StatusIconController statusIconController;
    private String username;
    private ConversationDto conversation;
    private boolean isGroup = false;

    @FXML
    private void initialize() {
        Object controllerObj = statusIcon.getProperties().get("fx:controller");
        if (controllerObj instanceof StatusIconController controller) {
            statusIconController = controller;
            statusIconController.setStatus(StatusIconController.Status.ONLINE);
        }

        statusIcon.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                showContextMenu(event.getScreenX(), event.getScreenY());
            }
        });
    }

    public void setConversationContext(ConversationDto convo) {
        this.conversation = convo;
        this.isGroup = "GROUP".equals(convo.getType());
    }

    private void showContextMenu(double x, double y) {
        ContextMenu contextMenu = new ContextMenu();
        // Use your CSS class for the content
        contextMenu.getStyleClass().add("transparent-context-menu");

        // --- CRITICAL FIX: STRIP THE ROOT PADDING/BACKGROUND ---
        contextMenu.setOnShown(e -> {
            Scene scene = contextMenu.getScene();
            if (scene != null && scene.getRoot() != null) {
                // This overrides the .root style from DiscordTheme.css that adds padding: 10
                scene.getRoot().setStyle(
                        "-fx-background-color: transparent; " +
                                "-fx-padding: 0; " +
                                "-fx-background-radius: 0; " +
                                "-fx-effect: null;"
                );
            }
        });

        Button actionBtn;

        if (isGroup) {
            actionBtn = new Button("Group Settings");
            actionBtn.getStyleClass().add("admin-action-button");
            actionBtn.setOnAction(e -> {
                contextMenu.hide();
                openGroupSettings();
            });
        } else {
            actionBtn = new Button("Report " + (username != null ? username : ""));
            actionBtn.getStyleClass().add("report-context-button");
            actionBtn.setOnAction(e -> {
                contextMenu.hide();
                openReportWindow();
            });
        }

        CustomMenuItem item = new CustomMenuItem(actionBtn);
        item.setHideOnClick(true);
        item.setStyle("-fx-background-color: transparent;");

        contextMenu.getItems().add(item);
        contextMenu.show(statusIcon, x, y);
    }

    private void openGroupSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/ui/fxml/GroupSettingsView.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof GroupSettingsViewController settingsCtrl) {
                settingsCtrl.setGroupData(this.conversation);
            }

            Stage stage = (Stage) statusIcon.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) { e.printStackTrace(); }
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
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void setData(String username, String fullName) {
        this.username = username;
        if (fullName != null && !fullName.isEmpty()) {
            nameLabel.setText(fullName + " (@" + username + ")");
        } else {
            nameLabel.setText(username);
        }
    }

    public void setNameStyle(String style) {
        nameLabel.setStyle(style);
    }

    public void setName(String name) { nameLabel.setText(name); }

    public void setStatus(StatusIconController.Status status) {
        if (statusIconController != null) statusIconController.setStatus(status);
    }

    public void setIcon(Image image) { if (statusIconController != null) statusIconController.setIcon(image); }
}