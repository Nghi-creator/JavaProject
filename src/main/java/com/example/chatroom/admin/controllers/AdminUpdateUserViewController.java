package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminUpdateUserViewController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private Button btnCancel;

    private AdminUserViewController.User currentUser;
    private AdminUserViewController parentController;

    public void setUserData(AdminUserViewController.User user) {
        this.currentUser = user;
        usernameField.setText(user.username);
        emailField.setText(user.email);
        addressField.setText(user.address);

        // Logic to split full name into First/Last for the UI
        String[] names = user.fullname.split(" ", 2);
        firstNameField.setText(names.length > 0 ? names[0] : "");
        lastNameField.setText(names.length > 1 ? names[1] : "");

        usernameField.setDisable(true); // Username cannot be changed
    }

    public void setParentController(AdminUserViewController parent) {
        this.parentController = parent;
    }

    @FXML
    private void handleConfirmUpdate() {
        // 1. Combine names
        String fullName = (firstNameField.getText() + " " + lastNameField.getText()).trim();

        // 2. Build JSON safely using JSONObject
        JSONObject json = new JSONObject();
        json.put("fullName", fullName);
        json.put("email", emailField.getText());
        json.put("address", addressField.getText());
        // Add other fields if your DTO requires them, even if empty
        json.put("username", currentUser.username);

        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + currentUser.id))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            System.out.println("Update successful!");
                            if (parentController != null) parentController.refreshTable();
                            closeWindow();
                        } else {
                            showAlert("Update Failed", "Server returned code: " + response.statusCode());
                            System.err.println("Failed: " + response.body());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Error", "Connection failed: " + e.getMessage()));
                        e.printStackTrace();
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred.");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}