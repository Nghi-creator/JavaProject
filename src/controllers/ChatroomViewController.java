package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ChatroomViewController {

    // This controller is for ChatroomView.fxml
    // It's already on the Dashboard, so it only needs to
    // handle clicks for OTHER screens.

    @FXML
    private void handleFriendsClick(ActionEvent event) {
        // Use the helper to switch to FriendsView.fxml
        SceneSwitcher.switchScene(event, "/fxml/FriendsView.fxml");
    }

    @FXML
    private void handleCreateGroupClick(ActionEvent event) {
        // Use the helper to switch to CreateGroupView.fxml
        SceneSwitcher.switchScene(event, "/fxml/CreateGroupView.fxml");
    }
    
    // We don't need a handleDashboardClick because we are already here.
}