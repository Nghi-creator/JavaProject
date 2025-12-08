package com.example.chatroom.user.controllers;

import com.example.chatroom.core.services.UserService;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        boolean success = UserService.login(username, password);

        if (success) {
            SceneSwitcher.switchScene(usernameField, "/user/ui/fxml/ChatroomView.fxml");
        } else {
            SceneSwitcher.showMessage("Login Failed");
        }
    }

    @FXML
    private void switchToSignup(MouseEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/SignupView.fxml");
    }

    @FXML
    private void resetPassword(MouseEvent event) {
        SceneSwitcher.showMessage("Password has been reset (not really)");
    }
}
