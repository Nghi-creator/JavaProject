package user.controllers;

import core.shared.controllers.HeaderController;
import core.shared.controllers.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class ChatroomViewController {

    @FXML private HeaderController headerController;

    @FXML
    private void initialize() {
        headerController.focusButton("chat");
    }

    public void displayGroupInfo(MouseEvent mouseEvent) {
        SceneSwitcher.openPopup("/user/ui/fxml/GroupInfoView.fxml", "User Info");
    }
}