package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.core.utils.TableDataManager; // <--- Using Shared Helper
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
                    // Re-trigger the predicate defined in step 3
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
                            masterData.clear();
                            JSONArray arr = new JSONArray(response.body());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                List<String> members = new ArrayList<>();
                                JSONArray mArr = obj.getJSONArray("memberUsernames");
                                for(int j=0; j<mArr.length(); j++) members.add(mArr.getString(j));

                                masterData.add(new Group(
                                        obj.getString("name"),
                                        obj.getString("createdAt"),
                                        obj.getString("adminUsername"),
                                        members
                                ));
                            }
                        }
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showDetails(Group group) {
        if(adminListView != null) {
            adminListView.getItems().clear();
            adminListView.getItems().add(group.adminUsername);
        }
        if(memberListView != null) {
            memberListView.getItems().clear();
            memberListView.getItems().addAll(group.members);
        }
    }

    private void setupColumn(TableColumn<Group, String> column, Callback<Group, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false);
    }

    public static class Group {
        String name, created, adminUsername;
        List<String> members;
        public Group(String n, String c, String admin, List<String> m) {
            this.name = n; this.created = c; this.adminUsername = admin; this.members = m;
        }
    }
}