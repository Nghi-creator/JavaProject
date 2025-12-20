package com.example.chatroom.user.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.chatroom.core.dto.UserDto;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CreateGroupViewController {

    @FXML private HeaderController headerController;
    @FXML private SearchBarController memberSearchController;
    @FXML private TextField groupNameField;
    @FXML private CheckBox encryptCheckbox; // <--- NEW: Encryption Checkbox
    @FXML private VBox resultContainer;     // Bottom list (Search Results)
    @FXML private VBox selectedContainer;   // Top list (Selected Members)

    // Tracks selected users: Key = UserId, Value = IsAdmin (boolean)
    private final Map<Integer, Boolean> selectedUsers = new HashMap<>();

    @FXML
    public void initialize() {
        if (headerController != null) {
            headerController.focusButton("createGroup");
        }
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
            String url = "http://" + serverIp + ":8080/api/users/search?q=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8) +
                    "&userId=" + ChatApp.currentUserId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            populateSearchList(response.body());
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateSearchList(String jsonBody) {
        resultContainer.getChildren().clear();
        JSONArray users = new JSONArray(jsonBody);

        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            int userId = user.getInt("id");
            String username = user.getString("username");
            String fullName = user.optString("fullName", "");

            try {
                // 1. Create the Row for Search Result
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(5));
                // Store userId in the row so we can find it later if we need to re-enable the button
                row.setUserData(userId);

                // Load NameCard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                Parent card = loader.load();
                NameCardController cardController = loader.getController();
                cardController.setData(username, fullName);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // "Add" Button
                Button addBtn = new Button(selectedUsers.containsKey(userId) ? "Added" : "+");
                addBtn.getStyleClass().add("add-friend-button");
                addBtn.setPrefWidth(80);

                if (selectedUsers.containsKey(userId)) {
                    addBtn.setDisable(true);
                }

                addBtn.setOnAction(e -> {
                    if (!selectedUsers.containsKey(userId)) {
                        addMemberToSelection(userId, username, fullName);
                        addBtn.setText("Added");
                        addBtn.setDisable(true);
                    }
                });

                row.getChildren().addAll(card, spacer, addBtn);
                resultContainer.getChildren().add(row);

            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void addMemberToSelection(int userId, String username, String fullName) {
        // 1. Add to Logic Map (Default: Not Admin)
        selectedUsers.put(userId, false);

        try {
            // 2. Create UI Row for "Selected Members" area
            HBox selectedRow = new HBox(10);
            selectedRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            selectedRow.setPadding(new javafx.geometry.Insets(5));
            selectedRow.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5px;");

            // Load NameCard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
            Parent card = loader.load();
            NameCardController cardController = loader.getController();
            cardController.setData(username, fullName);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // 3. "Make Admin" Button
            Button adminBtn = new Button("Make Admin");
            adminBtn.getStyleClass().add("normal-button"); 
            adminBtn.setPrefWidth(110);

            adminBtn.setOnAction(e -> toggleAdminStatus(userId, adminBtn));

            // 4. "Remove" Button (Cancel)
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("friend-action-button-danger");
            removeBtn.setPrefWidth(80);

            removeBtn.setOnAction(e -> removeFromSelection(userId, selectedRow));

            selectedRow.getChildren().addAll(card, spacer, adminBtn, removeBtn);
            selectedContainer.getChildren().add(selectedRow);

        } catch (IOException e) { e.printStackTrace(); }
    }

    private void toggleAdminStatus(int userId, Button btn) {
        boolean isCurrentlyAdmin = selectedUsers.get(userId);
        boolean newStatus = !isCurrentlyAdmin;

        selectedUsers.put(userId, newStatus);

        if (newStatus) {
            btn.setText("Revoke Admin");
            btn.setStyle("-fx-background-color: #3ba55c; -fx-text-fill: white;"); // Greenish for Active Admin
        } else {
            btn.setText("Make Admin");
            btn.setStyle(""); 
        }
    }

    private void removeFromSelection(int userId, HBox row) {
        // 1. Remove from Logic
        selectedUsers.remove(userId);

        // 2. Remove from UI (Top List)
        selectedContainer.getChildren().remove(row);

        for (Node node : resultContainer.getChildren()) {
            if (node instanceof HBox searchRow && searchRow.getUserData() instanceof Integer id) {
                if (id == userId) {
                    Node lastChild = searchRow.getChildren().get(searchRow.getChildren().size() - 1);
                    if (lastChild instanceof Button btn) {
                        btn.setText("+");
                        btn.setDisable(false);
                    }
                    break;
                }
            }
        }
    }

    @FXML
    private void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();

        if (groupName.isEmpty()) {
            showAlert("Error", "Group name is required.");
            return;
        }
        if (selectedUsers.isEmpty()) {
            showAlert("Error", "Please select at least one member.");
            return;
        }

        try {
            String serverIp = ConfigController.getServerIp();

            // 1. Build CSV for Member IDs
            StringBuilder memberIdsStr = new StringBuilder();
            StringBuilder adminIdsStr = new StringBuilder();

            int count = 0;
            for (Map.Entry<Integer, Boolean> entry : selectedUsers.entrySet()) {
                int id = entry.getKey();
                boolean isAdmin = entry.getValue();

                memberIdsStr.append(id);
                if (isAdmin) {
                    if (adminIdsStr.length() > 0) adminIdsStr.append(",");
                    adminIdsStr.append(id);
                }

                if (count < selectedUsers.size() - 1) memberIdsStr.append(",");
                count++;
            }

            String encodedName = URLEncoder.encode(groupName, StandardCharsets.UTF_8);

            // --- READ ENCRYPTION FLAG ---
            boolean isEncrypted = encryptCheckbox != null && encryptCheckbox.isSelected();

            String url = String.format("http://%s:8080/api/conversations/group?creatorId=%d&groupName=%s&memberIds=%s&adminIds=%s&isEncrypted=%b",
                    serverIp,
                    ChatApp.currentUserId,
                    encodedName,
                    memberIdsStr.toString(),
                    adminIdsStr.toString(),
                    isEncrypted 
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Group '" + groupName + "' created!");
                            SceneSwitcher.switchScene((Node) groupNameField, "/user/ui/fxml/ChatroomView.fxml",
                                    (ChatroomViewController controller) -> {
                                        controller.setCurrentUserId(ChatApp.currentUserId);
                                        controller.setWebSocketClient(ChatApp.chatWebSocketClient);
                                    }
                            );
                        } else {
                            showAlert("Error", "Failed to create group. Server Code: " + response.statusCode());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Connection Error", e.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        SceneSwitcher.switchScene((Node) event.getSource(), "/user/ui/fxml/ChatroomView.fxml",
                (ChatroomViewController controller) -> {
                    controller.setCurrentUserId(ChatApp.currentUserId);
                    controller.setWebSocketClient(ChatApp.chatWebSocketClient);
                }
        );
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void preAddMember(UserDto user) {
        Platform.runLater(() ->
                addMemberToSelection(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName()
                )
        );
    }

}