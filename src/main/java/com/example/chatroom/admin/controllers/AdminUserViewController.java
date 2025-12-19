package com.example.chatroom.admin.controllers;

import com.example.chatroom.core.model.User;
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
import java.time.LocalDate;
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
    private SortedList<User> sortedData;


    @FXML
    public void initialize() {
        headerController.focusButton("users");
        try { ConfigController.loadServerIp(); } catch (Exception ignored) {}

        sortCombo.setItems(FXCollections.observableArrayList(
                "Name (A–Z)",
                "Name (Z–A)",
                "Created (Newest)",
                "Created (Oldest)"
        ));

        setupColumn(colUsername, User::getUsername);
        setupColumn(colFullname, User::getFullName);
        setupColumn(colAddress, User::getAddress);
        setupColumn(colDob, u -> u.getDob() != null ? u.getDob().toString() : "");
        setupColumn(colEmail, User::getEmail);
        setupColumn(colGender, User::getGender);
        setupColumn(colStatus, User::getStatus);
        setupColumn(colCreated, u -> u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");

        setupActionColumn();
        loadDataFromServer();

        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        userTable.setItems(sortedData);

        if (searchBarController != null) {
            searchBarController.getSearchField()
                    .textProperty()
                    .addListener((o, ov, nv) -> applyFilters());
        }

        startDatePicker.valueProperty().addListener((o, ov, nv) -> applyFilters());
        endDatePicker.valueProperty().addListener((o, ov, nv) -> applyFilters());

        sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) applySort(newVal);
        });
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
        Platform.runLater(() -> {
            try {
                masterData.clear();
                JSONArray jsonArray = new JSONArray(responseBody);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    User user = new User();
                    user.setId(obj.getInt("id"));
                    user.setUsername(obj.getString("username"));
                    user.setFullName(obj.optString("fullName", ""));
                    user.setAddress(obj.optString("address", ""));
                    user.setEmail(obj.optString("email", ""));
                    user.setGender(obj.optString("gender", ""));
                    user.setStatus(obj.optString("status", ""));

                    if (!obj.isNull("dob"))
                        user.setDob(LocalDate.parse(obj.getString("dob")));

                    if (!obj.isNull("createdAt"))
                        user.setCreatedAt(java.time.LocalDateTime.parse(obj.getString("createdAt")));

                    masterData.add(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

                    // --- CONNECTED FRIENDS BUTTON ---
                    Button btnFriends = new Button("Friends");
                    btnFriends.getStyleClass().add("admin-action-button");
                    btnFriends.setOnAction(e -> openFriendsPopup(user));

                    Button btnHistory = new Button("History");
                    btnHistory.getStyleClass().add("admin-action-button");
                    btnHistory.setOnAction(e -> openHistoryPopup(user));

                    Button btnLock = new Button();
                    if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
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

    // --- POPUP HANDLERS ---

    // --- NEW METHOD FOR FRIENDS POPUP ---
    private void openFriendsPopup(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin/ui/fxml/AdminUserFriendsView.fxml"));
            Parent root = loader.load();

            AdminUserFriendsViewController controller = loader.getController();
            controller.loadFriends(user.getId(), user.getUsername());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Friends of " + user.getUsername());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load friends window.");
        }
    }

    @FXML
    private void openAddUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin/ui/fxml/AdminAddUserView.fxml"));
            Parent root = loader.load();

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
                    .uri(URI.create("http://" + serverIp + ":8080/api/users/" + user.getId() + "/lock"))
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + user.getUsername() + "?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            try {
                String serverIp = ConfigController.getServerIp();
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + serverIp + ":8080/api/users/" + user.getId()))
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

    private void applyFilters() {
        String searchText = searchBarController != null
                ? searchBarController.getSearchField().getText().toLowerCase()
                : "";

        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        filteredData.setPredicate(user -> {

            // --- text filter ---
            if (searchText != null && !searchText.isBlank()) {
                boolean match =
                        (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchText)) ||
                                (user.getFullName() != null && user.getFullName().toLowerCase().contains(searchText));
                if (!match) return false;
            }

            // --- date filter ---
            if (start == null && end == null) return true;
            if (user.getCreatedAt() == null) return false;

            LocalDate created = user.getCreatedAt().toLocalDate();

            if (start != null && created.isBefore(start)) return false;
            if (end != null && created.isAfter(end)) return false;

            return true;
        });
    }

    private void applySort(String sortOption) {

        Comparator<User> comparator;

        switch (sortOption) {
            case "Name (A–Z)":
                comparator = Comparator.comparing(
                        User::getFullName,
                        String.CASE_INSENSITIVE_ORDER
                );
                break;

            case "Name (Z–A)":
                comparator = Comparator.comparing(
                        User::getFullName,
                        String.CASE_INSENSITIVE_ORDER
                ).reversed();
                break;

            case "Created (Newest)":
                comparator = Comparator.comparing(
                        User::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed();
                break;

            case "Created (Oldest)":
                comparator = Comparator.comparing(
                        User::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                );
                break;

            default:
                return;
        }

        sortedData.setComparator(comparator);
    }



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
            controller.loadData(user.getUsername());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Login History: " + user.getUsername());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load history window.");
        }
    }
}