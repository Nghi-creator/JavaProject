package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ChatroomViewController {

    @FXML private HeaderController headerController;

    @FXML
    private void initialize() {
        headerController.focusButton("chat");
    }

    @FXML
    private void handleFriendsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/FriendsView.fxml");
    }

    @FXML
    private void handleCreateGroupClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/CreateGroupView.fxml");
    }

    @FXML
    private void handleNotificationClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/NotificationView.fxml");
    }

    @FXML
    private void handleAccountClick(ActionEvent actionEvent) {
    }

}