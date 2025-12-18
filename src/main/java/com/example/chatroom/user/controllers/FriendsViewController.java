package com.example.chatroom.user.controllers;

import com.example.chatroom.core.dto.UserDto;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FriendsViewController {

    @FXML private VBox friendListPane, requestsPane, findFriendsPane, blockedPane;
    @FXML private VBox friendListContainer, requestsListContainer, findFriendsListContainer, blockedListContainer;
    @FXML private Button btnFriendList, btnFriendRequests, btnFindFriends, btnBlocked;
    @FXML private SearchBarController searchBarController;

    // OPTIMIZATION: Reuse one client
    private static final HttpClient client = HttpClient.newHttpClient();

    @FXML
    private void initialize() {
        if (searchBarController != null) {
            searchBarController.setOnSearchListener(this::handleSearch);
        }
        showFriendList();
    }

    private void handleSearch(String query) {
        if (query.trim().isEmpty()) { findFriendsListContainer.getChildren().clear(); return; }
        fetchUsers("/api/friends/search?userId=" + ChatApp.currentUserId + "&q=" + query, findFriendsListContainer, "ADD");
    }

    @FXML private void showFriendList() {
        updateView(friendListPane, btnFriendList);
        fetchUsers("/api/friends?userId=" + ChatApp.currentUserId, friendListContainer, "FRIEND_ACTIONS");
    }

    @FXML private void showRequests() {
        updateView(requestsPane, btnFriendRequests);
        fetchUsers("/api/friends/requests?userId=" + ChatApp.currentUserId, requestsListContainer, "REQUEST_ACTIONS");
    }

    @FXML private void showFindFriends() {
        updateView(findFriendsPane, btnFindFriends);
        findFriendsListContainer.getChildren().clear();
    }

    @FXML private void showBlocked() {
        updateView(blockedPane, btnBlocked);
        if (blockedListContainer != null) {
            fetchUsers("/api/friends/blocked?userId=" + ChatApp.currentUserId, blockedListContainer, "BLOCKED_ACTIONS");
        }
    }

    private void fetchUsers(String endpoint, VBox container, String actionType) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://" + serverIp + ":8080" + endpoint)).GET().build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    container.getChildren().clear();
                    JSONArray arr = new JSONArray(res.body());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        UserDto user = new UserDto();
                        user.setId(obj.getInt("id"));
                        user.setUsername(obj.getString("username"));
                        user.setFullName(obj.optString("fullName", ""));
                        createRow(container, user, actionType);
                    }
                } else {
                    System.err.println("Fetch Failed: " + res.statusCode());
                }
            }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void createRow(VBox container, UserDto user, String actionType) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/shared/ui/fxml/NameCard.fxml"));
            Parent nameCard = loader.load();
            NameCardController controller = loader.getController();
            controller.setData(user.getUsername(), user.getFullName());

            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getChildren().add(nameCard);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().add(spacer);

            if (actionType.equals("ADD")) {
                Button btn = new Button("Add Friend");
                btn.getStyleClass().add("friend-action-button");
                btn.setOnAction(e -> sendAction("/api/friends/requests?senderId=" + ChatApp.currentUserId + "&receiverId=" + user.getId(), btn));
                row.getChildren().add(btn);
            } else if (actionType.equals("REQUEST_ACTIONS")) {
                Button btnAccept = new Button("Accept");
                btnAccept.getStyleClass().add("friend-action-button");
                btnAccept.setOnAction(e -> sendAction("/api/friends/requests/" + user.getId() + "?accept=true", null));

                Button btnDecline = new Button("Decline");
                btnDecline.getStyleClass().add("admin-danger-button");
                btnDecline.setOnAction(e -> sendAction("/api/friends/requests/" + user.getId() + "?accept=false", null));
                row.getChildren().addAll(btnAccept, btnDecline);
            } else if (actionType.equals("FRIEND_ACTIONS")) {
                Button btnUnfriend = new Button("Unfriend");
                btnUnfriend.getStyleClass().add("admin-danger-button");
                btnUnfriend.setOnAction(e -> sendAction("/api/friends/unfriend?userId=" + ChatApp.currentUserId + "&friendId=" + user.getId(), null));

                Button btnBlock = new Button("Block");
                btnBlock.getStyleClass().add("admin-danger-button");
                btnBlock.setOnAction(e -> sendAction("/api/friends/block?userId=" + ChatApp.currentUserId + "&targetId=" + user.getId(), null));
                row.getChildren().addAll(btnUnfriend, btnBlock);
            } else if (actionType.equals("BLOCKED_ACTIONS")) {
                Button btnUnblock = new Button("Unblock");
                btnUnblock.getStyleClass().add("friend-action-button");
                btnUnblock.setOnAction(e -> sendAction("/api/friends/unblock?userId=" + ChatApp.currentUserId + "&targetId=" + user.getId(), null));
                row.getChildren().add(btnUnblock);
            }

            container.getChildren().add(row);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendAction(String url, Button btn) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080" + url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    if (btn != null) { btn.setText("Sent"); btn.setDisable(true); }
                    else {
                        // REFRESH ALL VIEWS TO ENSURE CONSISTENCY
                        if (friendListPane.isVisible()) showFriendList();
                        if (requestsPane.isVisible()) showRequests();
                        if (blockedPane.isVisible()) showBlocked();
                    }
                } else {
                    // ALERT USER ON ERROR
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Action failed. Server Code: " + res.statusCode());
                    alert.show();
                }
            }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateView(VBox paneToShow, Button activeBtn) {
        friendListPane.setVisible(false); requestsPane.setVisible(false);
        findFriendsPane.setVisible(false); blockedPane.setVisible(false);
        paneToShow.setVisible(true);
        // ... (Keep existing styling)
        String inactive = "friends-nav-button"; String active = "friends-nav-button-selected";
        btnFriendList.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        btnFriendRequests.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        btnFindFriends.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        btnBlocked.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        activeBtn.getStyleClass().remove(inactive); activeBtn.getStyleClass().add(active);
    }
}