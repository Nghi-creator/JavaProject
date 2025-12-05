package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.HeaderController;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class NotificationViewController {

    @FXML private HeaderController headerController;

    @FXML
    private void initialize() {
        headerController.focusButton("notification");
    }

    @FXML
    private void handleChatClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/ChatroomView.fxml");
    }

    @FXML
    private void handleFriendsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/FriendsView.fxml");
    }

    @FXML
    private void handleCreateGroupClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/CreateGroupView.fxml");
    }

    @FXML
    private void handleAccountClick(ActionEvent actionEvent) {
    }

    @FXML
    private void toggleAll(ActionEvent event) {}

    @FXML
    private void toggleFriendRequests(ActionEvent event) {}

    @FXML
    private void toggleNewMessages(ActionEvent event) {}

    @FXML
    private void toggleGroupChatMentions(ActionEvent event) {}

    @FXML
    private void toggleGroupInvites(ActionEvent event) {}

    @FXML
    private void toggleSystem(ActionEvent event) {}
}