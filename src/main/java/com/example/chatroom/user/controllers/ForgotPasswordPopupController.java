package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

public class ForgotPasswordPopupController {

    @FXML private TextField emailField;

    @FXML
    private void handleConfirm() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showAlert("Error", "Please enter your email.");
            return;
        }

        try {
            String serverIp = ConfigController.getServerIp();

            JSONObject json = new JSONObject();
            json.put("email", email);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/forgot-password"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", response.body());
                            emailField.getScene().getWindow().hide();
                        } else if (response.statusCode() == 404) {
                            showAlert("Error", "Email not found.");
                        } else {
                            showAlert("Error", "Something went wrong. Try again later.");
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() ->
                                showAlert("Connection Error", "Could not reach server."));
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unexpected error.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
