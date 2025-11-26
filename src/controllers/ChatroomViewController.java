package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ChatroomViewController {

    @FXML private HeaderController headerController;

    @FXML
    private void initialize() {
        headerController.focusButton("chat");
    }

    public void displayGroupInfo(MouseEvent mouseEvent) {
        SceneSwitcher.openPopup("/fxml/UserInfoView.fxml", "User Info");
    }
}