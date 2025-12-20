package com.example.chatroom.user.controllers;

import com.example.chatroom.core.dto.ConversationDto;
import com.example.chatroom.core.dto.UserDto;
import com.example.chatroom.core.shared.controllers.*;
import com.example.chatroom.core.utils.JsonUtil;
import com.example.chatroom.user.ChatApp;
import com.example.chatroom.user.websocket.ChatWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FriendsViewController {

    @FXML private VBox friendListPane, requestsPane, findFriendsPane, blockedPane;
    @FXML private VBox friendListContainer, requestsListContainer, findFriendsListContainer, blockedListContainer;
    @FXML private Button btnFriendList, btnFriendRequests, btnFindFriends, btnBlocked;
    @FXML private ToggleButton btnOnlineOnly;
    private boolean filterOnlineOnly = false;
    private final Map<Integer, NameCardController> friendCards = new HashMap<>();

    @FXML private SearchBarController searchBarController;
    @FXML private SearchBarController friendSearchBarController;
    @FXML private SearchBarController requestSearchBarController;

    private static final HttpClient client = HttpClient.newHttpClient();

    private List<UserDto> allFriends = new ArrayList<>();
    private List<UserDto> allRequests = new ArrayList<>();

    ChatWebSocketClient webSocketClient;
    public void setWebSocketClient(ChatWebSocketClient client) {
        this.webSocketClient = client;
        client.setStatusCallback(this::onStatusUpdate);
    }

    @FXML
    private void initialize() {
        if (searchBarController != null) {
            searchBarController.setOnSearchListener(this::handleGlobalSearch);
        }
        if (friendSearchBarController != null) {
            friendSearchBarController.setOnSearchListener(query ->
                    filterList(query, allFriends, friendListContainer, "FRIEND_ACTIONS")
            );
        }
        if (requestSearchBarController != null) {
            requestSearchBarController.setOnSearchListener(query ->
                    filterList(query, allRequests, requestsListContainer, "REQUEST_ACTIONS")
            );
        }

        btnOnlineOnly.selectedProperty().addListener((obs, oldVal, newVal) -> {
            filterOnlineOnly = newVal;
            btnOnlineOnly.setText(newVal ? "Online" : "All");
            applyFriendFilters();
        });

        if (friendSearchBarController != null) {
            friendSearchBarController.setOnSearchListener(q -> applyFriendFilters());
        }

        showFriendList();
    }

    private void handleGlobalSearch(String query) {
        if (query.trim().isEmpty()) { findFriendsListContainer.getChildren().clear(); return; }
        fetchUsers("/api/friends/search?userId=" + ChatApp.currentUserId + "&q=" + query, findFriendsListContainer, "ADD", null);
    }

    private void filterList(String query, List<UserDto> sourceList, VBox container, String actionType) {
        String lowerQuery = query.toLowerCase();
        List<UserDto> filtered = sourceList.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(lowerQuery) ||
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());

        renderList(container, filtered, actionType);
    }

    @FXML private void showFriendList() {
        updateView(friendListPane, btnFriendList);
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
                    if (cacheList != null) {
                        cacheList.clear();
                        cacheList.addAll(fetchedData);
                    }
                    if (paneIsFriendList(container)) applyFriendFilters();
                    else renderList(container, fetchedData, actionType);
                }
            }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderList(VBox container, List<UserDto> users, String actionType) {
        friendCards.clear();
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

            controller.setStatus(
                    ChatApp.onlineUsers.contains(user.getId())
                            ? StatusIconController.Status.ONLINE
                            : StatusIconController.Status.OFFLINE
            );

            friendCards.put(user.getId(), controller);

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
                Button btnMessage = new Button("Message");
                btnMessage.getStyleClass().add("friend-action-button");
                btnMessage.setOnAction(e -> openOrCreateConversation(user));

                Button btnCreateGroup = new Button("Create Group");
                btnCreateGroup.getStyleClass().add("friend-action-button");
                btnCreateGroup.setOnAction(e -> openCreateGroupWithUser(user));

                Button btnUnfriend = new Button("Unfriend");
                btnUnfriend.getStyleClass().add("admin-danger-button");
                btnUnfriend.setOnAction(e -> sendAction("/api/friends?userId=" + ChatApp.currentUserId + "&friendId=" + user.getId(), "DELETE", null));

                Button btnBlock = new Button("Block");
                btnBlock.getStyleClass().add("admin-danger-button");
                btnBlock.setOnAction(e -> sendAction("/api/friends/block?userId=" + ChatApp.currentUserId + "&targetId=" + user.getId(), "POST", null));

                row.getChildren().addAll(btnMessage, btnCreateGroup, btnUnfriend, btnBlock);

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
                } else {
                    System.err.println("Action failed: " + res.statusCode() + " " + res.body());
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

    private void openOrCreateConversation(UserDto user) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/conversations/dm?userAId=" + ChatApp.currentUserId + "&userBId=" + user.getId()))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(json -> Platform.runLater(() -> {
                        try {
                            ConversationDto convo = JsonUtil.MAPPER.readValue(json, ConversationDto.class);
                            goToChatroom(convo, user);
                        } catch (Exception e) { e.printStackTrace(); }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void goToChatroom(ConversationDto convo, UserDto user) {
        VBox sourceNode = friendListPane;
        SceneSwitcher.switchScene(sourceNode, "/user/ui/fxml/ChatroomView.fxml", (ChatroomViewController controller) -> {
            controller.setCurrentUserId(ChatApp.currentUserId);
            controller.setWebSocketClient(ChatApp.chatWebSocketClient);
            Platform.runLater(() -> controller.selectConversation(convo, user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getUsername(), null));
        });
    }

    private void applyFriendFilters() {
        String query = (friendSearchBarController != null) ? friendSearchBarController.getSearchField().getText().toLowerCase() : "";
        List<UserDto> filtered = allFriends.stream()
                .filter(u -> (query.isBlank() || u.getUsername().toLowerCase().contains(query) || (u.getFullName() != null && u.getFullName().toLowerCase().contains(query))) &&
                        (!filterOnlineOnly || ChatApp.onlineUsers.contains(u.getId())))
                .toList();
        renderList(friendListContainer, filtered, "FRIEND_ACTIONS");
    }

    private boolean paneIsFriendList(VBox container) { return container == friendListContainer; }

    private void onStatusUpdate(Integer userId, boolean online) {
        if (online) ChatApp.onlineUsers.add(userId); else ChatApp.onlineUsers.remove(userId);
        NameCardController card = friendCards.get(userId);
        if (card != null) Platform.runLater(() -> card.setStatus(online ? StatusIconController.Status.ONLINE : StatusIconController.Status.OFFLINE));
        if (friendListPane.isVisible() && filterOnlineOnly) applyFriendFilters();
    }

    private void openCreateGroupWithUser(UserDto user) {
        VBox sourceNode = friendListPane;
        SceneSwitcher.switchScene(sourceNode, "/user/ui/fxml/CreateGroupView.fxml", (CreateGroupViewController controller) -> controller.preAddMember(user));
    }
}