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
    private FilteredList<Group> filteredData;

    @FXML
    public void initialize() {
        if (headerController != null) headerController.focusButton("groups");
        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Created Date (Newest)"));

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

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Group> sortedList = new SortedList<>(filteredData);
        sortedList.comparatorProperty().bind(groupTable.comparatorProperty());
        groupTable.setItems(sortedList);

        if (searchBarController != null) {
            searchBarController.setOnSearchListener(this::handleSearch);
        }

        sortCombo.setOnAction(e -> applySort());

        loadData();
    }

    private void loadData() {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();

            // --- FIX: CORRECT API ENDPOINT ---
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
                        } else {
                            System.err.println("Load Failed: " + response.statusCode());
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

    private void handleSearch(String query) {
        String lowerCaseQuery = query.toLowerCase();
        filteredData.setPredicate(group -> {
            if (query.isEmpty()) return true;
            return group.name.toLowerCase().contains(lowerCaseQuery);
        });
    }

    private void applySort() {
        String selected = sortCombo.getValue();
        if (selected == null) return;
        if (selected.equals("Name (A-Z)")) {
            FXCollections.sort(masterData, Comparator.comparing(g -> g.name.toLowerCase()));
        } else if (selected.equals("Created Date (Newest)")) {
            FXCollections.sort(masterData, (g1, g2) -> g2.created.compareTo(g1.created));
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
            this.name = n;
            this.created = c;
            this.adminUsername = admin;
            this.members = m;
        }
    }
}