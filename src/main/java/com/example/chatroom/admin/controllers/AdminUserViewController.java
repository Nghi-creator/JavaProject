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

        // This is the updated function with Lock/Delete logic
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

    // --- UPDATED ACTION COLUMN LOGIC ---
    private void setupActionColumn() {
        colAction.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    // Get the User object for this specific row
                    User user = getTableView().getItems().get(getIndex());

                    HBox buttons = new HBox(10);
                    buttons.setAlignment(Pos.CENTER_LEFT);

                    // 1. Update Button
                    Button btnUpdate = new Button("Update");
                    btnUpdate.getStyleClass().add("admin-action-button");
                    btnUpdate.setOnAction(e -> openUpdatePopup(user));

                    // 2. Friends Button (Visual Only)
                    Button btnFriends = new Button("Friends");
                    btnFriends.getStyleClass().add("admin-action-button");

                    // 3. History Button (Visual Only)
                    Button btnHistory = new Button("History");
                    btnHistory.getStyleClass().add("admin-action-button");

                    // 4. Lock Button (Now Functional!)
                    Button btnLock = new Button();
                    // Change text dynamically: If Locked -> Show "Unlock", If Active -> Show "Lock"
                    if ("LOCKED".equalsIgnoreCase(user.status)) {
                        btnLock.setText("Unlock");
                        // Optional: Add a different class for Unlock if you want green color
                        btnLock.getStyleClass().add("admin-action-button");
                    } else {
                        btnLock.setText("Lock");
                        btnLock.getStyleClass().add("admin-danger-button");
                    }
                    // Attach the Lock Action
                    btnLock.setOnAction(e -> handleLockUser(user));

                    // 5. Delete Button (Now Functional!)
                    Button btnDelete = new Button("Delete");
                    btnDelete.getStyleClass().add("admin-danger-button");
                    // Attach the Delete Action
                    btnDelete.setOnAction(e -> handleDeleteUser(user));

                    // Add all buttons to the HBox
                    buttons.getChildren().addAll(btnUpdate, btnFriends, btnHistory, btnLock, btnDelete);
                    setGraphic(buttons);
                }
            }
        });
    }

    // --- NEW API HANDLER METHODS ---

    private void handleLockUser(User user) {
        try {
            String serverIp = ConfigController.getServerIp();
            HttpClient client = HttpClient.newHttpClient();
            // Call the endpoint: PUT /api/users/{id}/lock
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + user.id + "/lock"))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            refreshTable(); // Refresh table to see the status change to LOCKED/ACTIVE
                        } else {
                            showAlert("Error", "Could not update user status. Server code: " + response.statusCode());
                        }
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> showAlert("Connection Error", e.getMessage()));
                        return null;
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteUser(User user) {
        // 1. Confirm with the admin before deleting
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Delete user: " + user.username + "?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // 2. If Yes, send DELETE request
            try {
                String serverIp = ConfigController.getServerIp();
                HttpClient client = HttpClient.newHttpClient();
                // Call the endpoint: DELETE /api/users/{id}
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + serverIp + ":8080/api/users/" + user.id))
                        .DELETE()
                        .build();

                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> Platform.runLater(() -> {
                            if (response.statusCode() == 200) {
                                masterData.remove(user); // Remove from the list immediately
                                System.out.println("User deleted successfully.");
                            } else {
                                showAlert("Error", "Could not delete user. Server code: " + response.statusCode());
                            }
                        }))
                        .exceptionally(e -> {
                            Platform.runLater(() -> showAlert("Connection Error", e.getMessage()));
                            return null;
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- END NEW METHODS ---

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