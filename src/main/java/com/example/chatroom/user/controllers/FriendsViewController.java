package com.example.chatroom.user.controllers;

import com.example.chatroom.core.dto.UserDto;
import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.NameCardController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.user.ChatApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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

    @FXML private VBox friendListPane;
    @FXML private VBox requestsPane;
    @FXML private VBox findFriendsPane;
    @FXML private VBox blockedPane;

    @FXML private VBox friendListContainer;
    @FXML private VBox requestsListContainer;
    @FXML private VBox findFriendsListContainer;

    @FXML private Button btnFriendList;
    @FXML private Button btnFriendRequests;
    @FXML private Button btnFindFriends;
    @FXML private Button btnBlocked;

    @FXML private SearchBarController searchBarController;

    @FXML
    private void initialize() {
        if (searchBarController != null) {
            searchBarController.setOnSearchListener(this::handleSearch);
        }
        showFriendList(); // Default view
    }

    private void handleSearch(String query) {
        if (query.trim().isEmpty()) {
            findFriendsListContainer.getChildren().clear();
            return;
        }
        fetchUsers("/api/friends/search?userId=" + ChatApp.currentUserId + "&q=" + query,
                findFriendsListContainer, "ADD");
    }

    @FXML private void showFriendList() {
        updateView(friendListPane, btnFriendList);
        fetchUsers("/api/friends?userId=" + ChatApp.currentUserId, friendListContainer, "NONE");
    }

    @FXML private void showRequests() {
        updateView(requestsPane, btnFriendRequests);
        fetchUsers("/api/friends/requests?userId=" + ChatApp.currentUserId, requestsListContainer, "ACCEPT");
    }

    @FXML private void showFindFriends() {
        updateView(findFriendsPane, btnFindFriends);
        findFriendsListContainer.getChildren().clear();
    }

    @FXML private void showBlocked() {
        updateView(blockedPane, btnBlocked);
    }

    private void fetchUsers(String endpoint, VBox container, String actionType) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080" + endpoint))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            container.getChildren().clear();
                            JSONArray arr = new JSONArray(response.body());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                UserDto user = new UserDto();
                                user.setId(obj.getInt("id"));
                                user.setUsername(obj.getString("username"));
                                user.setFullName(obj.optString("fullName", ""));
                                createRow(container, user, actionType);
                            }
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
                btn.setOnAction(e -> sendAction("/api/friends/request?senderId=" + ChatApp.currentUserId + "&receiverId=" + user.getId(), btn));
                row.getChildren().add(btn);
            } else if (actionType.equals("ACCEPT")) {
                Button btnAccept = new Button("Accept");
                btnAccept.getStyleClass().add("friend-action-button");
                btnAccept.setOnAction(e -> sendAction("/api/friends/requests/" + user.getId() + "?accept=true", null)); // ID here is RequestID
                Button btnDecline = new Button("Decline");
                btnDecline.getStyleClass().add("admin-danger-button");
                btnDecline.setOnAction(e -> sendAction("/api/friends/requests/" + user.getId() + "?accept=false", null));
                row.getChildren().addAll(btnAccept, btnDecline);
            }
            container.getChildren().add(row);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendAction(String url, Button btn) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://" + serverIp + ":8080" + url)).POST(HttpRequest.BodyPublishers.noBody()).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    if (btn != null) { btn.setText("Sent"); btn.setDisable(true); }
                    else { showRequests(); showFriendList(); } // Refresh
                }
            }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateView(VBox paneToShow, Button activeBtn) {
        friendListPane.setVisible(false); requestsPane.setVisible(false);
        findFriendsPane.setVisible(false); blockedPane.setVisible(false);
        paneToShow.setVisible(true);
        String inactive = "friends-nav-button"; String active = "friends-nav-button-selected";
        btnFriendList.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        btnFriendRequests.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        btnFindFriends.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        btnBlocked.getStyleClass().replaceAll(s -> s.equals(active) ? inactive : s);
        activeBtn.getStyleClass().remove(inactive); activeBtn.getStyleClass().add(active);
    }
}