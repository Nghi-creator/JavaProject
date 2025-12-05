package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        String jsonPayload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:8080/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Server response: " + response.body());

            if (response.statusCode() == 200) {
                 SceneSwitcher.switchScene(usernameField, "/user/ui/fxml/ChatroomView.fxml");
            } else {
                SceneSwitcher.showMessage("Login Failed");
            }

        } catch (Exception e) {
            e.printStackTrace();
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
