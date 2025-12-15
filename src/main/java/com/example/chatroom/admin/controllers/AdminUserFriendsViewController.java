package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminUserFriendsViewController {

    @FXML private Text titleText;
    @FXML private TableView<FriendshipRow> friendsTable;
    @FXML private TableColumn<FriendshipRow, String> colUsername;
    @FXML private TableColumn<FriendshipRow, String> colStatus;
    @FXML private TableColumn<FriendshipRow, String> colSince;

    private ObservableList<FriendshipRow> friendList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colSince.setCellValueFactory(new PropertyValueFactory<>("since"));

        friendsTable.setItems(friendList);
    }

    public void loadFriends(int userId, String username) {
        titleText.setText("Friends List: " + username);
        fetchData(userId);
    }

    private void fetchData(int userId) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            // Call the NEW endpoint
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/friends/details?userId=" + userId))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            friendList.clear();
                            JSONArray arr = new JSONArray(response.body());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                friendList.add(new FriendshipRow(
                                        obj.getString("username"),
                                        obj.getString("status"),
                                        obj.getString("since")
                                ));
                            }
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Helper class for TableView
    public static class FriendshipRow {
        private String username;
        private String status;
        private String since;

        public FriendshipRow(String u, String s, String d) {
            this.username = u; this.status = s; this.since = d;
        }
        public String getUsername() { return username; }
        public String getStatus() { return status; }
        public String getSince() { return since; }
    }
}