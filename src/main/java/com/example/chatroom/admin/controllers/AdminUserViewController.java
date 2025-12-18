package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.shared.controllers.ConfigController;
import com.example.chatroom.core.shared.controllers.SearchBarController;
import com.example.chatroom.core.utils.TableDataManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Comparator;

public class AdminUserViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername, colFullname, colAddress, colDob, colEmail, colGender, colStatus, colCreated, colAction;

    // --- MATCHING IDs FOR FXML ---
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> statusFilterCombo; // Renamed from filterCombo

    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private SearchBarController searchBarController;

    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private TableDataManager<User> tableManager;

    @FXML
    public void initialize() {
        if (headerController != null) headerController.focusButton("users");
        try { ConfigController.loadServerIp(); } catch (Exception ignored) {}

        // 1. Setup Sorting
        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Created Date (Newest)"));

        // 2. Setup Status Filter
        statusFilterCombo.setItems(FXCollections.observableArrayList("All Status", "Active", "Locked"));
        statusFilterCombo.getSelectionModel().select("All Status");

        // 3. Setup Columns
        setupColumn(colUsername, data -> data.username);
        setupColumn(colFullname, data -> data.fullname);
        setupColumn(colAddress, data -> data.address);
        setupColumn(colDob, data -> data.dob);
        setupColumn(colEmail, data -> data.email);
        setupColumn(colGender, data -> data.gender);
        setupColumn(colStatus, data -> data.status);
        setupColumn(colCreated, data -> data.createdAt);
        setupActionColumn();

        // 4. Initialize Table Manager
        tableManager = new TableDataManager<>(userTable, masterData);

        // 5. Define Sort Logic
        tableManager.addSortOption("Name (A-Z)", Comparator.comparing(u -> u.fullname.toLowerCase()));
        tableManager.addSortOption("Created Date (Newest)", (u1, u2) -> {
            String d1 = u1.createdAt == null ? "" : u1.createdAt;
            String d2 = u2.createdAt == null ? "" : u2.createdAt;
            return d2.compareTo(d1); // Descending
        });
        tableManager.setupSortController(sortCombo);

        // 6. Setup Listeners
        if (searchBarController != null) {
            searchBarController.getSearchField().textProperty().addListener((o, ov, nv) -> updateTableFilters());
        }
        statusFilterCombo.setOnAction(e -> updateTableFilters());
        startDatePicker.valueProperty().addListener((o, ov, nv) -> updateTableFilters());
        endDatePicker.valueProperty().addListener((o, ov, nv) -> updateTableFilters());

        loadDataFromServer();
    }

    public void refreshTable() {
        loadDataFromServer();
    }

    private void updateTableFilters() {
        String query = searchBarController != null ? searchBarController.getSearchField().getText().toLowerCase() : "";
        String statusSel = statusFilterCombo.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        tableManager.setFilterPredicate(user -> {
            // Strict Search: Name OR Username
            boolean matchText = user.fullname.toLowerCase().contains(query)
                    || user.username.toLowerCase().contains(query);

            // Status Filter
            boolean matchStatus = true;
            if (statusSel != null && !statusSel.equals("All Status")) {
                matchStatus = user.status.equalsIgnoreCase(statusSel);
            }

            // Date Range
            boolean matchDate = true;
            if (start != null || end != null) {
                LocalDate created = parseDate(user.createdAt);
                if (created != null) {
                    if (start != null && created.isBefore(start)) matchDate = false;
                    if (end != null && created.isAfter(end)) matchDate = false;
                }
            }

            return matchText && matchStatus && matchDate;
        });
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.equals("N/A")) return null;
            return LocalDate.parse(dateStr.split("T")[0]);
        } catch (Exception e) { return null; }
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
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) parseAndPopulateTable(response.body());
                    }));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void parseAndPopulateTable(String responseBody) {
        try {
            masterData.clear();
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                masterData.add(new User(
                        obj.getInt("id"),
                        obj.getString("username"),
                        obj.optString("fullName", "N/A"),
                        obj.optString("address", "N/A"),
                        obj.optString("dob", "N/A"),
                        obj.optString("email", "N/A"),
                        obj.optString("gender", "N/A"),
                        obj.optString("status", "ACTIVE"),
                        obj.optString("createdAt", "N/A")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- STANDARD UI SETUP ---
    private void setupColumn(TableColumn<User, String> column, Callback<User, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
    }

    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User user = getTableView().getItems().get(getIndex());
                HBox buttons = new HBox(10); buttons.setAlignment(Pos.CENTER_LEFT);

                Button btnUpdate = new Button("Update"); btnUpdate.getStyleClass().add("admin-action-button");
                btnUpdate.setOnAction(e -> openUpdatePopup(user));

                Button btnFriends = new Button("Friends"); btnFriends.getStyleClass().add("admin-action-button");
                btnFriends.setOnAction(e -> openFriendsPopup(user));

                Button btnHistory = new Button("History"); btnHistory.getStyleClass().add("admin-action-button");
                btnHistory.setOnAction(e -> openHistoryPopup(user));

                Button btnLock = new Button("LOCKED".equalsIgnoreCase(user.status) ? "Unlock" : "Lock");
                btnLock.getStyleClass().add("LOCKED".equalsIgnoreCase(user.status) ? "admin-action-button" : "admin-danger-button");
                btnLock.setOnAction(e -> handleLockUser(user));

                Button btnDelete = new Button("Delete"); btnDelete.getStyleClass().add("admin-danger-button");
                btnDelete.setOnAction(e -> handleDeleteUser(user));

                buttons.getChildren().addAll(btnUpdate, btnFriends, btnHistory, btnLock, btnDelete);
                setGraphic(buttons);
            }
        });
    }

    @FXML private void openAddUser(ActionEvent event) {
        openPopup("/admin/ui/fxml/AdminAddUserView.fxml", "Add New User", loader ->
                ((AdminAddUserViewController) loader.getController()).setParentController(this));
    }
    private void openUpdatePopup(User user) {
        openPopup("/admin/ui/fxml/AdminUpdateUserView.fxml", "Update User", loader -> {
            AdminUpdateUserViewController c = loader.getController(); c.setUserData(user); c.setParentController(this);
        });
    }
    private void openFriendsPopup(User user) {
        openPopup("/admin/ui/fxml/AdminUserFriendsView.fxml", "Friends", loader ->
                ((AdminUserFriendsViewController) loader.getController()).loadFriends(user.id, user.username));
    }
    private void openHistoryPopup(User user) {
        openPopup("/admin/ui/fxml/AdminUserHistoryView.fxml", "History", loader ->
                ((AdminUserHistoryViewController) loader.getController()).loadData(user.username));
    }
    private void openPopup(String fxml, String title, java.util.function.Consumer<FXMLLoader> configurer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml)); Parent root = loader.load();
            if (configurer != null) configurer.accept(loader);
            Stage stage = new Stage(); stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(title); stage.setScene(new Scene(root)); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
    private void handleLockUser(User user) { sendRequest("/api/users/" + user.id + "/lock", "POST"); }
    private void handleDeleteUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + user.username + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) { sendRequest("/api/users/" + user.id, "DELETE"); masterData.remove(user); }
    }
    private void sendRequest(String endpoint, String method) {
        try {
            String serverIp = ConfigController.getServerIp(); HttpClient client = HttpClient.newHttpClient();
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create("http://" + serverIp + ":8080" + endpoint));
            if (method.equals("DELETE")) builder.DELETE(); else builder.POST(HttpRequest.BodyPublishers.noBody());
            client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString()).thenAccept(res -> Platform.runLater(this::refreshTable));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class User {
        public int id;
        public String username, fullname, address, dob, email, gender, status, createdAt;
        public User(int id, String u, String f, String a, String d, String e, String g, String s, String c) {
            this.id = id; this.username = u; this.fullname = f; this.address = a; this.dob = d;
            this.email = e; this.gender = g; this.status = s; this.createdAt = c;
        }
    }
}