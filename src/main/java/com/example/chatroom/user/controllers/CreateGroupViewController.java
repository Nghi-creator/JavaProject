package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.HeaderController;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class CreateGroupViewController {

    @FXML private HeaderController headerController;

    @FXML
    private void initialize() {
        headerController.focusButton("createGroup");
    }

    @FXML
    private void handleDashboardClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/ChatroomView.fxml");
    }

    @FXML
    private void handleFriendsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/FriendsView.fxml");
    }

    @FXML
    private void handleNotificationClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/NotificationView.fxml");
    }

    @FXML
    private void handleAccountClick(ActionEvent actionEvent) {
    }

}