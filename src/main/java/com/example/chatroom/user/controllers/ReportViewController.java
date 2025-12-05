package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ReportViewController {

    @FXML private TextField reportTitleField;
    @FXML private TextArea reportDescriptionField;

    @FXML private Label reportedAccountLabel;

    public void setReportedAccount(String username) {
        reportedAccountLabel.setText(username);
    }

    @FXML
    private void handleBack() {
        SceneSwitcher.switchScene(reportTitleField, "/user/ui/fxml/ChatroomView.fxml");
    }

    @FXML
    private void handleSubmitReport() {
        String title = reportTitleField.getText();
        String desc = reportDescriptionField.getText();

        System.out.println("Report Submitted:");
        System.out.println("Title: " + title);
        System.out.println("Description: " + desc);
    }
}
