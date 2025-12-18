package com.example.chatroom.user.controllers;

import com.example.chatroom.core.dto.UserDto;
import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import com.example.chatroom.user.ChatApp;
import com.example.chatroom.user.PresenceWebSocketManager;
import com.example.chatroom.user.WebSocketManager; // <-- added
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

public class LoginViewController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all fields");
            return;
        }

        // Create Login JSON
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("password", password);

        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            try {
                                String responseBody = response.body();
                                JSONObject userJson = new JSONObject(responseBody);

                                // 1. Save ID
                                ChatApp.currentUserId = userJson.getInt("id");

                                // 2. Save User Details
                                UserDto user = new UserDto();
                                user.setUsername(userJson.getString("username"));
                                user.setEmail(userJson.getString("email"));
                                user.setFullName(userJson.optString("fullName", ""));
                                user.setAddress(userJson.optString("address", ""));
                                user.setGender(userJson.optString("gender", "OTHER"));

                                // 3. Handle Date Safely
                                String dobStr = userJson.optString("dob", null);
                                if (dobStr != null && !dobStr.isEmpty()) {
                                    if (dobStr.contains("T")) {
                                        dobStr = dobStr.split("T")[0];
                                    }
                                    user.setDob(LocalDate.parse(dobStr));
                                }

                                // 4. Save to Global Session
                                ChatApp.currentUser = user;

                                // 5. Connect WebSocket AFTER HTTP login
                                PresenceWebSocketManager.getInstance().connect(serverIp);


                                // 6. Switch Scene with controller consumer to set currentUserId
                                SceneSwitcher.switchScene(
                                        (javafx.scene.Node) event.getSource(),
                                        "/user/ui/fxml/ChatroomView.fxml",
                                        (com.example.chatroom.user.controllers.ChatroomViewController controller) -> {
                                            controller.setCurrentUserId(ChatApp.currentUserId);
                                        }
                                );


                            } catch (Exception e) {
                                e.printStackTrace();
                                showAlert("Error", "Failed to parse user data.");
                            }
                        } else {
                            showAlert("Login Failed", "Invalid username or password");
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Connection Error", "Could not connect to server: " + e.getMessage()));
                        return null;
                    });

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
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/ForgotPasswordView.fxml");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
