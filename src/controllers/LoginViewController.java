package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // Replace with your real logic
        System.out.println("Login: " + user + " / " + pass);
    }

    @FXML
    private void switchToSignup(MouseEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/fxml/SignupView.fxml");
    }

    public void resetPassword(MouseEvent event) {
    }
}
