package com.example.chatroom.core.shared.controllers;

import com.example.chatroom.user.ChatApp;
import com.example.chatroom.core.dto.UserDto;
import com.example.chatroom.core.dto.ChangePasswordRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;

public class SettingsViewController {

    // --- VIEW ONLY LABELS ---
    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    @FXML private Label lblFullName;
    @FXML private Label lblAddress;
    @FXML private Label lblDob;
    @FXML private Label lblGender;

    // --- EDIT FIELDS ---
    @FXML private VBox accountEditPanel;
    @FXML private VBox changePasswordPanel;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private DatePicker dobPicker;

    // --- PASSWORD FIELDS ---
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private UserDto currentUser;

    @FXML
    public void initialize() {
        genderCombo.setItems(FXCollections.observableArrayList("MALE", "FEMALE", "OTHER"));

        // Load global user
        this.currentUser = ChatApp.currentUser;

        if (currentUser != null) {
            populateFields();
        } else {
            System.err.println("User is null. Make sure you logged in correctly.");
        }

        // Hide edit panels initially
        accountEditPanel.setVisible(false);
        accountEditPanel.setManaged(false);
        changePasswordPanel.setVisible(false);
        changePasswordPanel.setManaged(false);
    }

    private void populateFields() {
        // 1. UPDATE THE STATIC VIEW LABELS
        lblUsername.setText(currentUser.getUsername());
        lblEmail.setText(currentUser.getEmail());
        lblFullName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        lblAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");
        lblDob.setText(currentUser.getDob() != null ? currentUser.getDob().toString() : "");
        lblGender.setText(currentUser.getGender() != null ? currentUser.getGender() : "");

        // 2. UPDATE THE EDIT FIELDS
        usernameField.setText(currentUser.getUsername());
        emailField.setText(currentUser.getEmail());
        addressField.setText(currentUser.getAddress());

        if (currentUser.getFullName() != null) {
            String[] names = currentUser.getFullName().split(" ", 2);
            firstNameField.setText(names.length > 0 ? names[0] : "");
            lastNameField.setText(names.length > 1 ? names[1] : "");
        }
        if (currentUser.getGender() != null) genderCombo.setValue(currentUser.getGender());
        if (currentUser.getDob() != null) dobPicker.setValue(currentUser.getDob());
    }

    // --- TOGGLE METHODS ---

    @FXML
    private void handleUpdateAccountInfo() {
        boolean isVisible = accountEditPanel.isVisible();
        accountEditPanel.setVisible(!isVisible);
        accountEditPanel.setManaged(!isVisible);
        changePasswordPanel.setVisible(false);
        changePasswordPanel.setManaged(false);
    }

    @FXML
    private void toggleChangePassword() {
        boolean isVisible = changePasswordPanel.isVisible();
        changePasswordPanel.setVisible(!isVisible);
        changePasswordPanel.setManaged(!isVisible);
        accountEditPanel.setVisible(false);
        accountEditPanel.setManaged(false);
    }

    // --- SAVE PROFILE LOGIC ---

    @FXML
    private void handleSaveProfile() {
        if (!isValidEmail(emailField.getText())) {
            showAlert("Error", "Invalid email format.");
            return;
        }

        String fullName = (firstNameField.getText() + " " + lastNameField.getText()).trim();

        // Build JSON
        JSONObject json = new JSONObject();
        json.put("fullName", fullName);
        json.put("email", emailField.getText());
        json.put("address", addressField.getText());
        json.put("gender", genderCombo.getValue());
        if (dobPicker.getValue() != null) {
            json.put("dob", dobPicker.getValue().toString());
        }

        sendUpdateRequest(ChatApp.currentUserId, json);
    }

    private void sendUpdateRequest(int userId, JSONObject json) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + userId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Profile updated!");

                            // UPDATE LOCAL DATA IMMEDIATELY
                            currentUser.setFullName(json.getString("fullName"));
                            currentUser.setEmail(json.getString("email"));
                            currentUser.setAddress(json.getString("address"));
                            currentUser.setGender(json.optString("gender"));
                            if (json.has("dob")) currentUser.setDob(LocalDate.parse(json.getString("dob")));

                            // REFRESH UI
                            populateFields();
                            handleUpdateAccountInfo(); // Close the panel
                        } else {
                            showAlert("Error", "Update failed. Server Code: " + response.statusCode());
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- CHANGE PASSWORD LOGIC ---

    @FXML
    private void handleChangePassword() {
        String oldPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        // Use the username from the text field, or fallback to the global user
        String username = usernameField.getText();
        if (username == null || username.isEmpty()) {
            if (currentUser != null) username = currentUser.getUsername();
        }

        // 1. CHECK FOR EMPTY FIELDS
        if (oldPass == null || oldPass.isEmpty() || newPass == null || newPass.isEmpty()) {
            showAlert("Error", "Please fill in all password fields, including Current Password.");
            return;
        }

        // 2. CHECK MATCH
        if (!newPass.equals(confirmPass)) {
            showAlert("Error", "New passwords do not match.");
            return;
        }

        // 3. CHECK IF NEW PASS IS SAME AS OLD
        if (oldPass.equals(newPass)) {
            showAlert("Error", "New password cannot be the same as the old one.");
            return;
        }

        // 4. PREPARE REQUEST
        ChangePasswordRequest requestDto = new ChangePasswordRequest(username, oldPass, newPass);
        JSONObject json = new JSONObject();
        json.put("username", requestDto.getUsername());
        json.put("oldPassword", requestDto.getOldPassword());
        json.put("newPassword", requestDto.getNewPassword());

        sendPasswordChangeRequest(ChatApp.currentUserId, json);
    }

    // --- MISSING METHOD ADDED HERE ---
    private void sendPasswordChangeRequest(int userId, JSONObject json) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + userId + "/password"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Password changed successfully!");
                            currentPasswordField.clear();
                            newPasswordField.clear();
                            confirmPasswordField.clear();
                            toggleChangePassword(); // Close panel
                        } else if (response.statusCode() == 401) {
                            showAlert("Error", "Incorrect current password.");
                        } else {
                            showAlert("Error", "Failed to change password. Server Code: " + response.statusCode());
                        }
                    }));
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Error", "Connection failed: " + e.getMessage()));
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/ChatroomView.fxml");
    }

//    @FXML
//    private void handleGeneratePassword() {
//        showAlert("Info", "This feature is coming soon!");
//    }

    // --- UTILS ---

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}