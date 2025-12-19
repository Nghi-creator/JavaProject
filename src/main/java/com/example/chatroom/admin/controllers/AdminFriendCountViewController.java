package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.core.utils.TableDataManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    @FXML private ComboBox<String> filterCountCombo;
    @FXML private TextField filterCountField;

    private ObservableList<UserStats> masterData = FXCollections.observableArrayList();
    private TableDataManager<UserStats> tableManager;

    @FXML
    public void initialize() {
        if (headerController != null) headerController.focusButton("friendCount");

        // 1. Setup Columns
        setupColumn(colUsername, data -> data.username);
        setupColumn(colFullname, data -> data.fullname);
        setupColumn(colEmail, data -> data.email);
        setupColumn(colGender, data -> data.gender);
        setupColumn(colAddress, data -> data.address);
        setupColumn(colDob, data -> data.dob);
        setupColumn(colFriends, data -> String.valueOf(data.friends));
        setupColumn(colFriendsOfFriends, data -> String.valueOf(data.friendsOfFriends));

        // 2. Initialize Table Manager
        tableManager = new TableDataManager<>(userTable, masterData);

        // 3. Setup Sorting
        tableManager.addSortOption("Name (A-Z)", Comparator.comparing(u -> u.fullname.toLowerCase()));
        tableManager.addSortOption("Most Friends", Comparator.comparingInt((UserStats u) -> u.friends).reversed());
        tableManager.addSortOption("Least Friends", Comparator.comparingInt((UserStats u) -> u.friends));
        tableManager.setupSortController(sortCombo);

        // 4. Setup "Smart" Number Filter (Added extra spaces for visual clarity)
        filterCountCombo.setItems(FXCollections.observableArrayList("Friends  >", "Friends  <", "Friends  ="));
        filterCountCombo.getSelectionModel().select("Friends  >"); // Default

        // 5. Add Listeners
        if (searchBarController != null) {
            searchBarController.getSearchField().textProperty().addListener((o, ov, nv) -> updateTableFilters());
        }
        filterCountCombo.setOnAction(e -> updateTableFilters());
        filterCountField.textProperty().addListener((o, ov, nv) -> {
            // Force numeric input only
            if (!nv.matches("\\d*")) filterCountField.setText(nv.replaceAll("[^\\d]", ""));
            updateTableFilters();
        });

        loadData();
    }

    private void updateTableFilters() {
        String query = searchBarController != null ? searchBarController.getSearchField().getText().toLowerCase() : "";
        String operator = filterCountCombo.getValue();
        String countInput = filterCountField.getText();

        tableManager.setFilterPredicate(user -> {
            // Text Search
            boolean matchText = user.username.toLowerCase().contains(query) ||
                    user.fullname.toLowerCase().contains(query) ||
                    user.email.toLowerCase().contains(query);

            // Number Filter
            boolean matchCount = true;
            if (countInput != null && !countInput.isEmpty()) {
                try {
                    int inputVal = Integer.parseInt(countInput);
                    if (operator.contains(">")) matchCount = user.friends > inputVal;
                    else if (operator.contains("<")) matchCount = user.friends < inputVal;
                    else if (operator.contains("=")) matchCount = user.friends == inputVal;
                } catch (NumberFormatException ignored) {}
            }

            return matchText && matchCount;
        });
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
                                        obj.optString("fullName", "N/A"),
                                        obj.optString("email", "N/A"),
                                        obj.optString("gender", "N/A"),
                                        obj.optString("address", "N/A"),
                                        obj.optString("dob", "N/A"),
                                        obj.getInt("friendCount"),
                                        obj.getInt("friendsOfFriendsCount")
                                ));
                            }
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupColumn(TableColumn<UserStats, String> column, Callback<UserStats, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
    }

    public static class UserStats {
        String username, fullname, email, gender, address, dob;
        int friends, friendsOfFriends;

        public UserStats(String u, String f, String e, String g, String a, String d, int friends, int fof) {
            this.username = u; this.fullname = f; this.email = e;
            this.gender = g; this.address = a; this.dob = d;
            this.friends = friends; this.friendsOfFriends = fof;
        }
    }
}