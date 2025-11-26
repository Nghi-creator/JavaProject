package controllers;

import javafx.event.ActionEvent;

public class UserInfoViewController {
    public void handleAddFriendClick(ActionEvent event) {
    }

    public void handleAddToGroupClick(ActionEvent event) {
    }

    public void handleBlockClick(ActionEvent event) {
    }

    public void handleReportClick(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/ReportView.fxml");
    }
}
