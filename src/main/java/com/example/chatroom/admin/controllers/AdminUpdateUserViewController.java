package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.model.User;
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

public class AdminUpdateUserViewController {

    @FXML private TextField firstNameField, lastNameField, usernameField, emailField, addressField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private Button btnCancel;

    private User currentUser;
    private AdminUserViewController parentController;

    @FXML
    public void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE", "OTHER"));
    }

    public void setUserData(User user) {
        this.currentUser = user;

        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        addressField.setText(user.getAddress());
        usernameField.setDisable(true);

        if (user.getFullName() != null) {
            String[] names = user.getFullName().split(" ", 2);
            firstNameField.setText(names.length > 0 ? names[0] : "");
            lastNameField.setText(names.length > 1 ? names[1] : "");
        }

        genderCombo.getSelectionModel()
                .select(user.getGender() != null ? user.getGender() : "OTHER");

        if (user.getDob() != null) {
            dobPicker.setValue(user.getDob());
        }
    }

    public void setParentController(AdminUserViewController parent) {
        this.parentController = parent;
    }

    @FXML
    private void handleConfirmUpdate() {
        String fullName = (firstNameField.getText() + " " + lastNameField.getText()).trim();

        if (!isValidEmail(emailField.getText())) {
            showAlert("Error", "Invalid email format.");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("username", currentUser.getUsername());
        json.put("fullName", fullName);
        json.put("email", emailField.getText());
        json.put("address", addressField.getText());
        json.put("gender", genderCombo.getValue());

        if (dobPicker.getValue() != null) {
            json.put("dob", dobPicker.getValue().toString());
        }

        String newPassword = passwordField.getText();
        if (newPassword != null && !newPassword.isBlank()) {
            json.put("password", newPassword);
        }

        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + currentUser.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            if (parentController != null) parentController.refreshTable();
                            closeWindow();
                        } else {
                            showAlert("Update Failed", "Server returned code: " + response.statusCode());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() ->
                                showAlert("Error", "Connection failed: " + e.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            showAlert("Error", "Unexpected error occurred.");
        }
    }

    @FXML
    private void closeWindow() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}
