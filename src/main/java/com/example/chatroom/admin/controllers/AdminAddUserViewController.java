package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

public class AdminAddUserViewController {

    @FXML private TextField firstNameField, lastNameField, usernameField, emailField, addressField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private Button btnCancel;

    private AdminUserViewController parentController;

    @FXML
    public void initialize() {
        // Initialize Gender options
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE", "OTHER"));
        genderCombo.getSelectionModel().select("OTHER"); // Default
    }

    public void setParentController(AdminUserViewController parent) {
        this.parentController = parent;
    }

    @FXML
    private void handleConfirmAdd() {
        if (usernameField.getText().isEmpty() || emailField.getText().isEmpty()) {
            showAlert("Error", "Username and Email are required.");
            return;
        }

        if (dobPicker.getValue() != null && dobPicker.getValue().isAfter(LocalDate.now())) {
            showAlert("Error", "Date of Birth cannot be in the future.");
            return;
        }

        if (!isValidEmail(emailField.getText())) {
            showAlert("Error", "Invalid email format (e.g., name@example.com).");
            return;
        }

        String fullName = (firstNameField.getText() + " " + lastNameField.getText()).trim();

        JSONObject json = new JSONObject();
        json.put("username", usernameField.getText());
        json.put("fullName", fullName);
        json.put("email", emailField.getText());
        json.put("address", addressField.getText());
        json.put("password", "123456");
        json.put("status", "ACTIVE");

        // Add Gender & DOB
        json.put("gender", genderCombo.getValue());
        if (dobPicker.getValue() != null) {
            json.put("dob", dobPicker.getValue().toString()); // Sends "YYYY-MM-DD"
        }

        sendPostRequest(json);
    }

    private void sendPostRequest(JSONObject json) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            if (parentController != null) parentController.refreshTable();
                            closeWindow();
                        } else {
                            showAlert("Error", "Failed. Code: " + response.statusCode());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Connection Error", e.getMessage()));
                        return null;
                    });
        } catch (Exception e) { e.printStackTrace(); }
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

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}