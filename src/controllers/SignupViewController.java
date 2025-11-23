package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

public class SignupViewController {

    @FXML
    private void handleSignup(ActionEvent event) {}

    @FXML
    private void switchToLogin(MouseEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/LoginView.fxml");
    }
}
