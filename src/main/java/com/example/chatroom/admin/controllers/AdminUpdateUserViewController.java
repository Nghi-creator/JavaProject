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

public class AdminUpdateUserViewController {

    @FXML private TextField firstNameField, lastNameField, usernameField, emailField, addressField;
    @FXML private PasswordField passwordField; // <--- NEW FIELD
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;
    @FXML private Button btnCancel;

    private AdminUserViewController.User currentUser;
    private AdminUserViewController parentController;

    @FXML
    public void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE", "OTHER"));
    }

    public void setUserData(AdminUserViewController.User user) {
        this.currentUser = user;
        usernameField.setText(user.username);
        emailField.setText(user.email);
        addressField.setText(user.address);
        usernameField.setDisable(true); // Username cannot be changed

        String[] names = user.fullname.split(" ", 2);
        firstNameField.setText(names.length > 0 ? names[0] : "");
        lastNameField.setText(names.length > 1 ? names[1] : "");

        if (user.gender != null && !user.gender.isEmpty()) {
            genderCombo.getSelectionModel().select(user.gender);
        } else {
            genderCombo.getSelectionModel().select("OTHER");
        }

        if (user.dob != null && !user.dob.isEmpty()) {
            try {
                String datePart = user.dob.split("T")[0];
                dobPicker.setValue(LocalDate.parse(datePart));
            } catch (Exception e) {
                System.err.println("Could not parse DOB: " + user.dob);
            }
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
        json.put("fullName", fullName);
        json.put("email", emailField.getText());
        json.put("address", addressField.getText());
        json.put("username", currentUser.username);

        // ONLY SEND PASSWORD IF NOT EMPTY
        String newPass = passwordField.getText();
        if (newPass != null && !newPass.isEmpty()) {
            json.put("password", newPass);
        }

        json.put("gender", genderCombo.getValue());
        if (dobPicker.getValue() != null) {
            json.put("dob", dobPicker.getValue().toString());
        }

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
                            if (parentController != null) parentController.refreshTable();
                            closeWindow();
                        } else {
                            showAlert("Update Failed", "Server returned code: " + response.statusCode());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Error", "Connection failed: " + e.getMessage()));
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

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }
}