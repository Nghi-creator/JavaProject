package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class CreateGroupViewController {

    // This controller is for CreateGroupView.fxml

    @FXML
    private void handleDashboardClick(ActionEvent event) {
        // Use the helper to switch to ChatroomView.fxml
        SceneSwitcher.switchScene(event, "/fxml/ChatroomView.fxml");
    }

    @FXML
    private void handleFriendsClick(ActionEvent event) {
        // Use the helper to switch to FriendsView.fxml
        SceneSwitcher.switchScene(event, "/fxml/FriendsView.fxml");
    }
    
    // We don't need a handleCreateGroupClick because we are already here.
}