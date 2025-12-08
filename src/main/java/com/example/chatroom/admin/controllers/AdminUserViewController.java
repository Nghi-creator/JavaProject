package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminUserViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername, colFullname, colAddress, colDob, colEmail, colGender, colStatus, colCreated, colAction;
    @FXML private ComboBox<String> sortCombo, filterCombo;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private SearchBarController searchBarController;

    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;

    @FXML
    public void initialize() {
        headerController.focusButton("users");
        try { ConfigController.loadServerIp(); } catch (Exception ignored) {}

        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Created Date (Newest)"));
        filterCombo.setItems(FXCollections.observableArrayList("Name", "Username", "Status"));

        setupColumn(colUsername, data -> data.username);
        setupColumn(colFullname, data -> data.fullname);
        setupColumn(colAddress, data -> data.address);
        setupColumn(colDob, data -> data.dob);
        setupColumn(colEmail, data -> data.email);
        setupColumn(colGender, data -> data.gender);
        setupColumn(colStatus, data -> data.status);
        setupColumn(colCreated, data -> data.createdAt);

        setupActionColumn();
        loadDataFromServer();

        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<User> sortedList = new SortedList<>(filteredData);
        sortedList.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedList);

        // Add listeners for search/filter
        searchBarController.searchField.textProperty().addListener((o, ov, nv) -> applyFilters());
    }

    private void loadDataFromServer() {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users"))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(this::parseAndPopulateTable)
                    .exceptionally(e -> { e.printStackTrace(); return null; });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseAndPopulateTable(String responseBody) {
        javafx.application.Platform.runLater(() -> {
            try {
                masterData.clear();
                JSONArray jsonArray = new JSONArray(responseBody);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    masterData.add(new User(
                            obj.getInt("id"),
                            obj.getString("username"),
                            obj.optString("fullName", ""),
                            obj.optString("address", ""),
                            obj.optString("dob", ""),
                            obj.optString("email", ""),
                            obj.optString("gender", ""),
                            obj.optString("status", ""),
                            obj.optString("createdAt", "")
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void setupColumn(TableColumn<User, String> column, Callback<User, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10);
                    buttons.setAlignment(Pos.CENTER_LEFT);

                    // 1. Update Button
                    Button btnUpdate = new Button("Update");
                    btnUpdate.getStyleClass().add("admin-action-button");
                    btnUpdate.setOnAction(e -> openUpdatePopup(getTableView().getItems().get(getIndex())));

                    // 2. Friends Button (Visual Only for now)
                    Button btnFriends = new Button("Friends");
                    btnFriends.getStyleClass().add("admin-action-button");

                    // 3. History Button (Visual Only for now)
                    Button btnHistory = new Button("History");
                    btnHistory.getStyleClass().add("admin-action-button");

                    // 4. Lock Button (Visual Only for now)
                    Button btnLock = new Button("Lock");
                    btnLock.getStyleClass().add("admin-danger-button");

                    // 5. Delete Button
                    Button btnDelete = new Button("Delete");
                    btnDelete.getStyleClass().add("admin-danger-button");

                    // Add all buttons to the HBox
                    buttons.getChildren().addAll(btnUpdate, btnFriends, btnHistory, btnLock, btnDelete);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void openUpdatePopup(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin/ui/fxml/AdminUpdateUserView.fxml"));
            Parent root = loader.load();

            AdminUpdateUserViewController controller = loader.getController();
            controller.setUserData(user);
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Update User");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshTable() { loadDataFromServer(); }
    private void applyFilters() { /* Logic handled in listener */ }
    @FXML private void openPasswordRequests(ActionEvent event) {}
    @FXML private void openAddUser(ActionEvent event) {}

    public static class User {
        public int id;
        public String username, fullname, address, dob, email, gender, status, createdAt;

        public User(int id, String u, String f, String a, String d, String e, String g, String s, String c) {
            this.id = id; this.username = u; this.fullname = f;
            this.address = a; this.dob = d;
            this.email = e; this.gender = g;
            this.status = s; this.createdAt = c;
        }
    }
}