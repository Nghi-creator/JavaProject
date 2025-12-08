package com.example.chatroom.user.controllers;

import com.example.chatroom.core.dto.SignupRequest;
import com.example.chatroom.core.services.UserService;
import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class SignupViewController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private TextField addressField;
    @FXML private DatePicker dobPicker;
    @FXML private ComboBox<String> genderSelector;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label fullNameError;
    @FXML private Label addressError;
    @FXML private Label dobError;
    @FXML private Label genderError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;

    @FXML
    private void initialize() {
        dobPicker.setEditable(true);
        genderSelector.getSelectionModel().select(0);
    }

    @FXML
    private void handleSignup(ActionEvent event) {
        clearErrors();

        boolean valid = validateFields();

        if (!valid) return;

        boolean emailAvailable = UserService.checkEmailAvailability(emailField.getText().trim());
        if (!emailAvailable) {
            SceneSwitcher.showMessage("Email already in use");
        }

        SignupRequest payload = new SignupRequest(
                usernameField.getText().trim(),
                passwordField.getText().trim(),
                fullNameField.getText().trim(),
                emailField.getText().trim(),
                genderSelector.getValue(),
                dobPicker.getValue(),
                addressField.getText().trim()
        );

        boolean success = UserService.signup(payload);

        if (success) {
            SceneSwitcher.switchScene(usernameField, "/user/ui/fxml/LoginView.fxml");
        } else {
            SceneSwitcher.showMessage("Signup failed");
        }
    }

    private boolean validateFields() {
        boolean valid = true;

        if (usernameField.getText().trim().isEmpty()) {
            usernameError.setText("Required");
            usernameError.setVisible(true);
            valid = false;
        }

        if (fullNameField.getText().trim().isEmpty()) {
            fullNameError.setText("Required");
            fullNameError.setVisible(true);
            valid = false;
        }

        if (addressField.getText().trim().isEmpty()) {
            addressError.setText("Required");
            addressError.setVisible(true);
            valid = false;
        }

        if (dobPicker.getValue() == null) {
            dobError.setText("Select a date");
            dobError.setVisible(true);
            valid = false;
        }

        if (genderSelector.getValue() == null || genderSelector.getValue().isEmpty()) {
            genderError.setText("Select gender");
            genderError.setVisible(true);
            valid = false;
        }

        if (emailField.getText().trim().isEmpty()) {
            emailError.setText("Required");
            emailError.setVisible(true);
            valid = false;
        } else if (!isValidEmail(emailField.getText().trim())) {
            emailError.setText("Invalid email");
            emailError.setVisible(true);
            valid = false;
        }

        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (password.isEmpty()) {
            passwordError.setText("Required");
            passwordError.setVisible(true);
            valid = false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordError.setText("Required");
            confirmPasswordError.setVisible(true);
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordError.setText("Passwords do not match");
            confirmPasswordError.setVisible(true);
            valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        usernameError.setVisible(false);
        emailError.setVisible(false);
        fullNameError.setVisible(false);
        addressError.setVisible(false);
        dobError.setVisible(false);
        genderError.setVisible(false);
        passwordError.setVisible(false);
        confirmPasswordError.setVisible(false);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    @FXML
    private void switchToLogin(MouseEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/LoginView.fxml");
    }
}