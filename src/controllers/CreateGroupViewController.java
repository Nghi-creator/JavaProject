package controllers;

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
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/ChatroomView.fxml");
    }

    @FXML
    private void handleFriendsClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/FriendsView.fxml");
    }

    @FXML
    private void handleNotificationClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/NotificationView.fxml");
    }

    @FXML
    private void handleAccountClick(ActionEvent actionEvent) {
    }

}