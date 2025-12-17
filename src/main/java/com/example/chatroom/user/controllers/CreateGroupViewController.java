package com.example.chatroom.user.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.HeaderController;
import com.example.chatroom.core.shared.controllers.NameCardController;
import com.example.chatroom.core.shared.controllers.SceneSwitcher;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.user.ChatApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CreateGroupViewController {

    @FXML private HeaderController headerController;
    @FXML private SearchBarController memberSearchController;
    @FXML private TextField groupNameField;
    @FXML private VBox resultContainer;

    private List<Integer> selectedUserIds = new ArrayList<>();

    @FXML
    public void initialize() {
        headerController.focusButton("createGroup");

        if (memberSearchController != null) {
            memberSearchController.setOnSearchListener(this::performSearch);
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            resultContainer.getChildren().clear();
            return;
        }

        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/search?q=" + query.replace(" ", "%20") + "&userId=" + ChatApp.currentUserId))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            populateList(response.body());
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateList(String jsonBody) {
        resultContainer.getChildren().clear();
        JSONArray users = new JSONArray(jsonBody);

        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            int userId = user.getInt("id");
            String username = user.getString("username");
            String fullName = user.optString("fullName", "");

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                Parent card = loader.load();
                NameCardController cardController = loader.getController();
                cardController.setData(username, fullName);

                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Button actionBtn = new Button(selectedUserIds.contains(userId) ? "Added" : "+");
                actionBtn.getStyleClass().add("add-friend-button");
                if (selectedUserIds.contains(userId)) actionBtn.setDisable(true);

                actionBtn.setOnAction(e -> {
                    if (!selectedUserIds.contains(userId)) {
                        selectedUserIds.add(userId);
                        actionBtn.setText("Added");
                        actionBtn.setDisable(true);
                    }
                });

                row.getChildren().addAll(card, actionBtn);
                resultContainer.getChildren().add(row);

            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();
        if (groupName.isEmpty()) {
            showAlert("Error", "Group name is required.");
            return;
        }
        if (selectedUserIds.isEmpty()) {
            showAlert("Error", "Please select at least one member.");
            return;
        }

        JSONObject json = new JSONObject();
        json.put("adminId", ChatApp.currentUserId);
        json.put("name", groupName);
        json.put("memberIds", selectedUserIds);

        sendCreateRequest(json);
    }

    private void sendCreateRequest(JSONObject json) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/groups"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Group '" + groupNameField.getText() + "' created!");

                            // --- FIX: Use SceneSwitcher directly instead of HeaderController ---
                            SceneSwitcher.switchScene(groupNameField, "/user/ui/fxml/ChatroomView.fxml");
                        } else {
                            showAlert("Error", "Failed to create group.");
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // Go back to dashboard
        SceneSwitcher.switchScene((Node) event.getSource(), "/user/ui/fxml/ChatroomView.fxml");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}