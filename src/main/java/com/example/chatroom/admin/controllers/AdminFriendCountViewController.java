package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;

public class AdminFriendCountViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private SearchBarController searchBarController;
    @FXML private TableView<UserStats> userTable;
    @FXML private TableColumn<UserStats, String> colUsername, colFullname, colEmail, colGender, colAddress, colDob, colFriends, colFriendsOfFriends;
    @FXML private ComboBox<String> sortCombo;

    private ObservableList<UserStats> masterData = FXCollections.observableArrayList();
    private FilteredList<UserStats> filteredData;

    @FXML
    public void initialize() {
        headerController.focusButton("friendCount");

        // Setup Columns
        setupColumn(colUsername, data -> data.username);
        setupColumn(colFullname, data -> data.fullname);
        setupColumn(colEmail, data -> data.email);       // <--- Added
        setupColumn(colGender, data -> data.gender);     // <--- Added
        setupColumn(colAddress, data -> data.address);
        setupColumn(colDob, data -> data.dob);
        setupColumn(colFriends, data -> String.valueOf(data.friends));
        setupColumn(colFriendsOfFriends, data -> String.valueOf(data.friendsOfFriends));

        // Sorting
        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Most Friends"));
        sortCombo.setOnAction(e -> applySort());

        // Setup Data and Search
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<UserStats> sortedList = new SortedList<>(filteredData);
        sortedList.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedList);

        if (searchBarController != null) {
            searchBarController.setOnSearchListener(this::handleSearch);
        }

        loadData();
    }

    private void loadData() {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/friends/admin/stats"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            masterData.clear();
                            JSONArray arr = new JSONArray(response.body());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                masterData.add(new UserStats(
                                        obj.getString("username"),
                                        obj.optString("fullName", ""),
                                        obj.optString("email", ""),
                                        obj.optString("gender", ""),
                                        obj.optString("address", ""),
                                        obj.optString("dob", ""),
                                        obj.getInt("friendCount"),
                                        obj.getInt("friendsOfFriendsCount")
                                ));
                            }
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleSearch(String query) {
        String lowerCaseQuery = query.toLowerCase();
        filteredData.setPredicate(user -> {
            if (query.isEmpty()) return true;
            return user.username.toLowerCase().contains(lowerCaseQuery) ||
                    user.fullname.toLowerCase().contains(lowerCaseQuery) ||
                    user.email.toLowerCase().contains(lowerCaseQuery);
        });
    }

    private void applySort() {
        String selected = sortCombo.getValue();
        if (selected == null) return;

        if (selected.equals("Name (A-Z)")) {
            FXCollections.sort(masterData, Comparator.comparing(u -> u.fullname.toLowerCase()));
        } else if (selected.equals("Most Friends")) {
            FXCollections.sort(masterData, Comparator.comparingInt((UserStats u) -> u.friends).reversed());
        }
    }

    private void setupColumn(TableColumn<UserStats, String> column, Callback<UserStats, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
    }

    // Helper Class
    public static class UserStats {
        String username, fullname, email, gender, address, dob;
        int friends, friendsOfFriends;

        public UserStats(String u, String f, String e, String g, String a, String d, int friends, int fof) {
            this.username = u;
            this.fullname = f;
            this.email = e;
            this.gender = g;
            this.address = a;
            this.dob = d;
            this.friends = friends;
            this.friendsOfFriends = fof;
        }
    }
}