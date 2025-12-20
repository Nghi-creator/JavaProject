package com.example.chatroom.user.controllers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ForgotPasswordViewController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML
    private void handleSubmit(ActionEvent event) {
        String user = usernameField.getText();
        String email = emailField.getText();
        String pass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showAlert("Error", "Please fill all fields");
            return;
        }
        if (!pass.equals(confirm)) {
            showAlert("Error", "Passwords do not match");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", user);
        json.put("oldPassword", email);
        json.put("newPassword", pass);

        sendRequest(json, event);
    }

    private void sendRequest(JSONObject json, ActionEvent event) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/forgot-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Request sent! Please wait for Admin approval.");
                            SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/LoginView.fxml");
                        } else {
                            showAlert("Error", "Failed: " + response.body());
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/LoginView.fxml");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}