package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.application.Platform;
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
import java.util.Optional;

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
                    User user = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(10);
                    buttons.setAlignment(Pos.CENTER_LEFT);

                    Button btnUpdate = new Button("Update");
                    btnUpdate.getStyleClass().add("admin-action-button");
                    btnUpdate.setOnAction(e -> openUpdatePopup(user));

                    Button btnFriends = new Button("Friends");
                    btnFriends.getStyleClass().add("admin-action-button");

                    Button btnHistory = new Button("History");
                    btnHistory.getStyleClass().add("admin-action-button");
                    btnHistory.setOnAction(e -> openHistoryPopup(user));

                    Button btnLock = new Button();
                    if ("LOCKED".equalsIgnoreCase(user.status)) {
                        btnLock.setText("Unlock");
                        btnLock.getStyleClass().add("admin-action-button");
                    } else {
                        btnLock.setText("Lock");
                        btnLock.getStyleClass().add("admin-danger-button");
                    }
                    btnLock.setOnAction(e -> handleLockUser(user));

                    Button btnDelete = new Button("Delete");
                    btnDelete.getStyleClass().add("admin-danger-button");
                    btnDelete.setOnAction(e -> handleDeleteUser(user));

                    buttons.getChildren().addAll(btnUpdate, btnFriends, btnHistory, btnLock, btnDelete);
                    setGraphic(buttons);
                }
            }
        });
    }

    // --- POPUP HANDLERS (New Implementation) ---

    @FXML
    private void openAddUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin/ui/fxml/AdminAddUserView.fxml"));
            Parent root = loader.load();

            // Get controller and pass 'this' so it can refresh the table on success
            AdminAddUserViewController controller = loader.getController();
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add New User");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load Add User window: " + e.getMessage());
        }
    }

    @FXML
    private void openPasswordRequests(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin/ui/fxml/AdminPasswordRequestsView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Password Requests");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load Password Requests window.");
        }
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

    // --- API ACTIONS ---

    private void handleLockUser(User user) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + user.id + "/lock"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) refreshTable();
                        else showAlert("Error", "Status update failed.");
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + user.username + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            try {
                String serverIp = ConfigController.getServerIp();
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + serverIp + ":8080/api/users/" + user.id))
                        .DELETE()
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> Platform.runLater(() -> {
                            if (response.statusCode() == 200) masterData.remove(user);
                            else showAlert("Error", "Delete failed.");
                        }));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void refreshTable() { loadDataFromServer(); }
    private void applyFilters() { /* Logic handled in listener */ }
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openHistoryPopup(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin/ui/fxml/AdminUserHistoryView.fxml"));
            Parent root = loader.load();

            AdminUserHistoryViewController controller = loader.getController();
            controller.loadData(user.username);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Login History: " + user.username);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load history window.");
        }
    }
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