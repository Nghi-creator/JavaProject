package com.example.user.controllers;

import com.example.core.shared.controllers.SceneSwitcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.input.MouseEvent;

public class SignupViewController {

    @FXML private DatePicker dobPicker;
    @FXML private ComboBox genderSelector;

    @FXML
    private void initialize() {
        dobPicker.setEditable(true);
        Platform.runLater(() -> dobPicker.getEditor().setPromptText("Select a date"));
        genderSelector.getSelectionModel().select(0);
    }

    @FXML
    private void handleSignup(ActionEvent event) {}

    @FXML
    private void switchToLogin(MouseEvent event) {
        SceneSwitcher.switchScene((javafx.scene.Node) event.getSource(), "/user/ui/fxml/LoginView.fxml");
    }
}
