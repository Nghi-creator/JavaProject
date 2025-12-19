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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdminGroupViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private SearchBarController searchBarController;
    @FXML private TableView<Group> groupTable;
    @FXML private TableColumn<Group, String> colGroupName, colCreated, colAction;
    @FXML private ComboBox<String> sortCombo;

    @FXML private ListView<String> adminListView;
    @FXML private ListView<String> memberListView;

    private ObservableList<Group> masterData = FXCollections.observableArrayList();
    private TableDataManager<Group> tableManager;

    @FXML
    public void initialize() {
        if (headerController != null) headerController.focusButton("groups");

        // 1. Setup Columns
        setupColumn(colGroupName, d -> d.name);
        setupColumn(colCreated, d -> d.created);

        colAction.setReorderable(false);
        colAction.setSortable(false);
        colAction.setCellFactory(new Callback<>() {
            @Override public TableCell<Group, String> call(TableColumn<Group, String> param) {
                return new TableCell<>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!empty) {
                            Button btn = new Button("Detail");
                            btn.getStyleClass().add("admin-action-button");
                            btn.setOnAction(e -> showDetails(getTableView().getItems().get(getIndex())));
                            setGraphic(btn);
                        } else { setGraphic(null); }
                    }
                };
            }
        });

        // 2. Initialize Table Manager
        tableManager = new TableDataManager<>(groupTable, masterData);

        // 3. Search Logic (Name Only)
        tableManager.setFilterPredicate(group -> {
            String query = searchBarController != null ? searchBarController.getSearchField().getText().toLowerCase() : "";
            if (query.isEmpty()) return true;
            return group.name.toLowerCase().contains(query);
        });

        // 4. Sort Logic
        tableManager.addSortOption("Name (A-Z)", Comparator.comparing(g -> g.name.toLowerCase()));
        tableManager.addSortOption("Created Date (Newest)", (g1, g2) -> g2.created.compareTo(g1.created));
        tableManager.setupSortController(sortCombo);

        // 5. Connect Search Bar
        if (searchBarController != null) {
            searchBarController.getSearchField().textProperty().addListener((o, ov, nv) ->
                    tableManager.setFilterPredicate(group -> group.name.toLowerCase().contains(nv.toLowerCase()))
            );
        }

        loadData();
    }

    private void loadData() {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/conversations/groups/all"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            System.out.println("DEBUG SERVER RESPONSE: " + response.body()); // <--- CHECK CONSOLE FOR THIS
                            masterData.clear();
                            try {
                                JSONArray arr = new JSONArray(response.body());
                                for (int i = 0; i < arr.length(); i++) {
                                    JSONObject obj = arr.getJSONObject(i);

                                    // --- 1. ROBUST ADMIN PARSING ---
                                    List<String> admins = new ArrayList<>();
                                    if (obj.has("adminUsernames") && !obj.isNull("adminUsernames")) {
                                        JSONArray adminArr = obj.getJSONArray("adminUsernames");
                                        for (int j = 0; j < adminArr.length(); j++) admins.add(adminArr.getString(j));
                                    }
                                    else if (obj.has("adminUsername") && !obj.isNull("adminUsername")) {
                                        // Fallback for old server version
                                        admins.add(obj.getString("adminUsername"));
                                    }

                                    // --- 2. ROBUST MEMBER PARSING ---
                                    List<String> members = new ArrayList<>();
                                    String memberKey = "memberUsernames"; // Default old key
                                    if (obj.has("memberNames")) memberKey = "memberNames"; // New key

                                    if (obj.has(memberKey) && !obj.isNull(memberKey)) {
                                        JSONArray mArr = obj.getJSONArray(memberKey);
                                        for (int j = 0; j < mArr.length(); j++) members.add(mArr.getString(j));
                                    }

                                    // Add to table data
                                    masterData.add(new Group(
                                            obj.optString("groupName", obj.optString("name", "Unknown")), // Check both keys
                                            obj.optString("createdAt", ""),
                                            admins,
                                            members
                                    ));
                                }
                            } catch (Exception e) {
                                System.err.println("JSON PARSING ERROR: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            System.err.println("SERVER ERROR: Code " + response.statusCode());
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showDetails(Group group) {
        // Update Admin List
        if(adminListView != null) {
            adminListView.getItems().clear();
            if (group.adminUsernames != null) {
                adminListView.getItems().addAll(group.adminUsernames);
            }
        }
        // Update Member List
        if(memberListView != null) {
            memberListView.getItems().clear();
            if (group.members != null) {
                memberListView.getItems().addAll(group.members);
            }
        }
    }

    private void setupColumn(TableColumn<Group, String> column, Callback<Group, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false);
    }

    public static class Group {
        String name, created;
        List<String> adminUsernames; // CHANGED: Now a list
        List<String> members;

        public Group(String n, String c, List<String> admins, List<String> m) {
            this.name = n;
            this.created = c;
            this.adminUsernames = admins;
            this.members = m;
        }
    }
}