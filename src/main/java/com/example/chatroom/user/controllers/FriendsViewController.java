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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FriendsViewController {

    @FXML private VBox friendListPane, requestsPane, findFriendsPane, blockedPane;
    @FXML private VBox friendListContainer, requestsListContainer, findFriendsListContainer, blockedListContainer;
    @FXML private Button btnFriendList, btnFriendRequests, btnFindFriends, btnBlocked;

    // --- SEARCH CONTROLLERS (Injected via fx:include) ---
    @FXML private SearchBarController searchBarController;       // Existing (Find Friends)
    @FXML private SearchBarController friendSearchBarController; // NEW (All Friends)
    @FXML private SearchBarController requestSearchBarController;// NEW (Requests)

    private static final HttpClient client = HttpClient.newHttpClient();

    // --- LOCAL CACHE (For instant filtering) ---
    private List<UserDto> allFriends = new ArrayList<>();
    private List<UserDto> allRequests = new ArrayList<>();

    @FXML
    private void initialize() {
        // 1. Global Search (Find New Friends) - API based
        if (searchBarController != null) {
            searchBarController.setOnSearchListener(this::handleGlobalSearch);
        }

        // 2. Local Search (Filter Friend List) - Memory based
        if (friendSearchBarController != null) {
            friendSearchBarController.setOnSearchListener(query ->
                    filterList(query, allFriends, friendListContainer, "FRIEND_ACTIONS")
            );
        }

        // 3. Local Search (Filter Requests) - Memory based
        if (requestSearchBarController != null) {
            requestSearchBarController.setOnSearchListener(query ->
                    filterList(query, allRequests, requestsListContainer, "REQUEST_ACTIONS")
            );
        }

        showFriendList();
    }

    private void handleGlobalSearch(String query) {
        if (query.trim().isEmpty()) { findFriendsListContainer.getChildren().clear(); return; }
        fetchUsers("/api/friends/search?userId=" + ChatApp.currentUserId + "&q=" + query, findFriendsListContainer, "ADD", null);
    }

    // --- FILTER LOGIC ---
    private void filterList(String query, List<UserDto> sourceList, VBox container, String actionType) {
        String lowerQuery = query.toLowerCase();
        List<UserDto> filtered = sourceList.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(lowerQuery) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());

        renderList(container, filtered, actionType);
    }

    // --- NAVIGATION ---
    @FXML private void showFriendList() {
        updateView(friendListPane, btnFriendList);
        // Clear search bar when switching tabs
        if (friendSearchBarController != null) friendSearchBarController.getSearchField().clear();
        fetchUsers("/api/friends?userId=" + ChatApp.currentUserId, friendListContainer, "FRIEND_ACTIONS", allFriends);
    }

    @FXML private void showRequests() {
        updateView(requestsPane, btnFriendRequests);
        if (requestSearchBarController != null) requestSearchBarController.getSearchField().clear();
        fetchUsers("/api/friends/requests?userId=" + ChatApp.currentUserId, requestsListContainer, "REQUEST_ACTIONS", allRequests);
    }

    @FXML private void showFindFriends() {
        updateView(findFriendsPane, btnFindFriends);
        findFriendsListContainer.getChildren().clear();
    }

    @FXML private void showBlocked() {
        updateView(blockedPane, btnBlocked);
        if (blockedListContainer != null) {
            fetchUsers("/api/friends/blocked?userId=" + ChatApp.currentUserId, blockedListContainer, "BLOCKED_ACTIONS", null);
        }
    }

    // --- DATA FETCHING ---
    private void fetchUsers(String endpoint, VBox container, String actionType, List<UserDto> cacheList) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://" + serverIp + ":8080" + endpoint)).GET().build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    List<UserDto> fetchedData = new ArrayList<>();
                    JSONArray arr = new JSONArray(res.body());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        UserDto user = new UserDto();
                        user.setId(obj.getInt("id"));
                        user.setUsername(obj.getString("username"));
                        user.setFullName(obj.optString("fullName", ""));
                        fetchedData.add(user);
                    }

                    // Update Cache
                    if (cacheList != null) {
                        cacheList.clear();
                        cacheList.addAll(fetchedData);
                    }

                    renderList(container, fetchedData, actionType);
                }
            }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderList(VBox container, List<UserDto> users, String actionType) {
        container.getChildren().clear();
        for (UserDto user : users) {
            createRow(container, user, actionType);
        }
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
                btn.setOnAction(e -> sendAction("/api/friends/requests?senderId=" + ChatApp.currentUserId + "&receiverId=" + user.getId(), "POST", btn));
                row.getChildren().add(btn);

            } else if (actionType.equals("REQUEST_ACTIONS")) {
                Button btnAccept = new Button("Accept");
                btnAccept.getStyleClass().add("friend-action-button");
                btnAccept.setOnAction(e -> sendAction("/api/friends/requests/" + user.getId() + "/response?accept=true", "POST", null));

                Button btnDecline = new Button("Decline");
                btnDecline.getStyleClass().add("admin-danger-button");
                btnDecline.setOnAction(e -> sendAction("/api/friends/requests/" + user.getId() + "/response?accept=false", "POST", null));
                row.getChildren().addAll(btnAccept, btnDecline);

            } else if (actionType.equals("FRIEND_ACTIONS")) {
                Button btnUnfriend = new Button("Unfriend");
                btnUnfriend.getStyleClass().add("admin-danger-button");
                btnUnfriend.setOnAction(e -> sendAction("/api/friends?userId=" + ChatApp.currentUserId + "&friendId=" + user.getId(), "DELETE", null));

                Button btnBlock = new Button("Block");
                btnBlock.getStyleClass().add("admin-danger-button");
                btnBlock.setOnAction(e -> sendAction("/api/friends/block?userId=" + ChatApp.currentUserId + "&targetId=" + user.getId(), "POST", null));
                row.getChildren().addAll(btnUnfriend, btnBlock);

            } else if (actionType.equals("BLOCKED_ACTIONS")) {
                Button btnUnblock = new Button("Unblock");
                btnUnblock.getStyleClass().add("friend-action-button");
                btnUnblock.setOnAction(e -> sendAction("/api/friends/block?userId=" + ChatApp.currentUserId + "&targetId=" + user.getId(), "DELETE", null));
                row.getChildren().add(btnUnblock);
            }

            container.getChildren().add(row);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendAction(String url, String method, Button btn) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create("http://" + serverIp + ":8080" + url));
            if (method.equals("POST")) builder.POST(HttpRequest.BodyPublishers.noBody());
            else if (method.equals("DELETE")) builder.DELETE();

            client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(() -> {
                if (res.statusCode() == 200) {
                    if (btn != null) { btn.setText("Sent"); btn.setDisable(true); }
                    else {
                        if (friendListPane.isVisible()) showFriendList();
                        if (requestsPane.isVisible()) showRequests();
                        if (blockedPane.isVisible()) showBlocked();
                    }
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