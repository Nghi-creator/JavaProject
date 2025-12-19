package com.example.chatroom.user.controllers;

import com.example.chatroom.core.dto.ConversationDto;
import com.example.chatroom.core.dto.ConversationDto.MemberDto;
import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.NameCardController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.user.ChatApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GroupSettingsViewController {

    @FXML private TextField groupNameField;
    @FXML private SearchBarController memberSearchController;
    @FXML private VBox memberListContainer;
    @FXML private VBox searchResultsContainer;
    @FXML private Button deleteGroupBtn;

    private ConversationDto conversation;
    private boolean isCurrentUserAdmin = false;
    private List<MemberDto> currentMembers = new ArrayList<>();

    @FXML
    public void initialize() {
        if (memberSearchController != null) {
            memberSearchController.setOnSearchListener(this::performSearch);
        }
    }

    public void setGroupData(ConversationDto convo) {
        this.conversation = convo;
        this.groupNameField.setText(convo.getName());

        if (convo.getMembers() != null) {
            this.currentMembers = new ArrayList<>(convo.getMembers());
        }

        this.isCurrentUserAdmin = false;
        if (ChatApp.currentUserId > 0 && currentMembers != null) {
            this.isCurrentUserAdmin = currentMembers.stream()
                    .anyMatch(m -> m.getId().equals(ChatApp.currentUserId)
                            && "ADMIN".equalsIgnoreCase(m.getRole()));
        }

        if (deleteGroupBtn != null) {
            deleteGroupBtn.setVisible(isCurrentUserAdmin);
            deleteGroupBtn.setManaged(isCurrentUserAdmin);
        }

        renderMemberList();
    }

    private void renderMemberList() {
        memberListContainer.getChildren().clear();

        for (MemberDto member : currentMembers) {
            try {
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(5));
                // Normal background for everyone
                row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5px;");

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                Parent card = loader.load();
                NameCardController cardController = loader.getController();

                String displayName = member.getFullName() != null ? member.getFullName() : member.getUsername();
                if ("ADMIN".equalsIgnoreCase(member.getRole())) displayName += " [Admin]";
                cardController.setData(displayName, null);

                // Check if this is YOU
                boolean isMe = member.getId().equals(ChatApp.currentUserId);
                if (isMe) {
                    // Turn name Text to BRIGHT GREEN
                    cardController.setNameStyle("-fx-text-fill: #3ba55c; -fx-font-weight: bold;");
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                row.getChildren().addAll(card, spacer);

                if (isCurrentUserAdmin && !isMe) {
                    boolean isTargetAdmin = "ADMIN".equalsIgnoreCase(member.getRole());
                    Button roleBtn = new Button(isTargetAdmin ? "Revoke Admin" : "Make Admin");
                    roleBtn.getStyleClass().add("normal-button");
                    roleBtn.setOnAction(e -> toggleAdmin(member, roleBtn));

                    Button kickBtn = new Button("Kick");
                    kickBtn.getStyleClass().add("friend-action-button-danger");
                    kickBtn.setOnAction(e -> removeMember(member));

                    row.getChildren().addAll(roleBtn, kickBtn);
                }
                else if (isMe) {
                    Button meBtn = new Button("You");
                    meBtn.setDisable(true);
                    meBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-opacity: 0.8; -fx-font-weight: bold;");
                    row.getChildren().add(meBtn);
                }

                memberListContainer.getChildren().add(row);

            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            searchResultsContainer.getChildren().clear();
            return;
        }

        String serverIp = ConfigController.getServerIp();
        HttpClient client = HttpClient.newHttpClient();
        String url = "http://" + serverIp + ":8080/api/users/search?q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&userId=" + ChatApp.currentUserId;

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) populateSearchResults(response.body());
                }));
    }

    private void populateSearchResults(String jsonBody) {
        searchResultsContainer.getChildren().clear();
        JSONArray users = new JSONArray(jsonBody);

        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.getJSONObject(i);
            int userId = user.getInt("id");
            if (currentMembers.stream().anyMatch(m -> m.getId().equals(userId))) continue;

            String username = user.getString("username");
            String fullName = user.optString("fullName", "");

            try {
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(5));

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
                Parent card = loader.load();
                NameCardController cardController = loader.getController();
                cardController.setData(username, fullName);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button addBtn = new Button("+");
                addBtn.getStyleClass().add("add-friend-button");
                addBtn.setOnAction(e -> addNewMember(userId, username, fullName, row));

                row.getChildren().addAll(card, spacer, addBtn);
                searchResultsContainer.getChildren().add(row);

            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void addNewMember(int id, String username, String fullName, HBox row) {
        MemberDto newMember = new MemberDto();
        newMember.setId(id);
        newMember.setUsername(username);
        newMember.setFullName(fullName);
        newMember.setRole("MEMBER");

        currentMembers.add(newMember);
        searchResultsContainer.getChildren().remove(row);
        renderMemberList();
    }

    private void toggleAdmin(MemberDto member, Button btn) {
        if ("ADMIN".equalsIgnoreCase(member.getRole())) {
            member.setRole("MEMBER");
        } else {
            member.setRole("ADMIN");
        }
        renderMemberList();
    }

    private void removeMember(MemberDto member) {
        currentMembers.remove(member);
        renderMemberList();
    }

    @FXML
    private void handleSaveChanges() {
        try {
            String serverIp = ConfigController.getServerIp();
            String newName = groupNameField.getText().trim();
            if (newName.isEmpty()) {
                showAlert("Error", "Group name cannot be empty");
                return;
            }

            StringBuilder memberIds = new StringBuilder();
            StringBuilder adminIds = new StringBuilder();

            for (int i = 0; i < currentMembers.size(); i++) {
                MemberDto m = currentMembers.get(i);
                memberIds.append(m.getId());
                if (i < currentMembers.size() - 1) memberIds.append(",");

                if ("ADMIN".equalsIgnoreCase(m.getRole())) {
                    if (adminIds.length() > 0) adminIds.append(",");
                    adminIds.append(m.getId());
                }
            }

            String url = String.format("http://%s:8080/api/conversations/group/%d?groupName=%s&memberIds=%s&adminIds=%s",
                    serverIp,
                    conversation.getId(),
                    URLEncoder.encode(newName, StandardCharsets.UTF_8),
                    memberIds.toString(),
                    adminIds.toString()
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showAlert("Success", "Group updated successfully!");
                            handleBack();
                        } else {
                            showAlert("Error", "Failed to update group. Code: " + response.statusCode());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Error", e.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred");
        }
    }

    @FXML
    private void handleDeleteGroup() {
        if (!isCurrentUserAdmin) return;
        String serverIp = ConfigController.getServerIp();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + serverIp + ":8080/api/conversations/" +
                        conversation.getId() + "?userId=" + ChatApp.currentUserId))
                .DELETE()
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(r -> Platform.runLater(this::handleBack));
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user/ui/fxml/ChatroomView.fxml"));
            Parent root = loader.load();
            ChatroomViewController chatController = loader.getController();
            chatController.setCurrentUserId(ChatApp.currentUserId);
            Stage stage = (Stage) groupNameField.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not navigate back: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}