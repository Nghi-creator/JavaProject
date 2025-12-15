package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.user.ChatApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ReportUserViewController {

    @FXML private TextArea reasonArea;
    @FXML private Text titleText;

    private String reportedUsername;

    public void setReportedUser(String username) {
        this.reportedUsername = username;
        titleText.setText("Report User: " + username);
    }

    @FXML
    private void handleSubmit() {
        String reason = reasonArea.getText().trim();
        if (reason.isEmpty()) {
            showAlert("Error", "Please enter a reason.");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("reporterId", ChatApp.currentUserId);
        json.put("reportedUsername", reportedUsername);
        json.put("reason", reason);

        sendReport(json);
    }

    private void sendReport(JSONObject json) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/reports"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Report submitted. Thank you.");
                            closeWindow();
                        } else {
                            showAlert("Error", "Failed to submit report.");
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        Stage stage = (Stage) reasonArea.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}