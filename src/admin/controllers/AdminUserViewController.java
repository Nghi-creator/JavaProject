package admin.controllers;

import core.shared.controllers.SearchBarController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class AdminUserViewController {

    @FXML private AdminHeaderController headerController;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername, colFullname, colAddress, colDob, colEmail, colGender, colStatus, colCreated, colAction;
    @FXML private ComboBox<String> sortCombo, filterCombo;
    @FXML private DatePicker startDatePicker, endDatePicker;

    @FXML private SearchBarController searchBarController;
    @FXML private TextField searchField;

    private ObservableList<User> masterData;
    private FilteredList<User> filteredData;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        headerController.focusButton("users");

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

        masterData = FXCollections.observableArrayList(
                new User("john_doe", "John Doe", "123 Street, NY", "1990-01-01", "john@example.com", "Male", "Active", "2023-01-10"),
                new User("jane_smith", "Jane Smith", "456 Avenue, CA", "1992-05-10", "jane@test.com", "Female", "Active", "2023-02-15"),
                new User("bobby_g", "Bob Gamer", "789 Road, TX", "1995-11-20", "bob@game.net", "Male", "Locked", "2023-03-01")
        );

        filteredData = new FilteredList<>(masterData, p -> true);

        searchBarController.searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());

        SortedList<User> sortedList = new SortedList<>(filteredData);
        sortedList.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedList);

        sortCombo.setOnAction(e -> applySort());
    }

    private void setupColumn(TableColumn<User, String> column, Callback<User, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false);
    }

    private void setupActionColumn() {
        colAction.setSortable(false);
        colAction.setReorderable(false);
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<User, String> call(TableColumn<User, String> param) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox buttons = new HBox(10);
                            buttons.setAlignment(Pos.CENTER_LEFT);

                            Button btnUpdate = new Button("Update");
                            btnUpdate.getStyleClass().add("admin-action-button");
                            btnUpdate.setOnAction(e -> AdminSceneSwitcher.openPopup("/admin/ui/fxml/AdminUpdateUserView.fxml", "Update User"));

                            Button btnFriends = new Button("Friends");
                            btnFriends.getStyleClass().add("admin-action-button");
                            btnFriends.setOnAction(e -> AdminSceneSwitcher.openPopup("/admin/ui/fxml/AdminUserFriendsView.fxml", "Friend List"));

                            Button btnHistory = new Button("History");
                            btnHistory.getStyleClass().add("admin-action-button");
                            btnHistory.setOnAction(e -> AdminSceneSwitcher.openPopup("/admin/ui/fxml/AdminUserHistoryView.fxml", "Login History"));

                            Button btnLock = new Button("Lock");
                            btnLock.getStyleClass().add("admin-danger-button"); 
                            btnLock.setOnAction(e -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Lock User");
                                alert.setHeaderText(null);
                                alert.setContentText("User account locked.");
                                DialogPane dialogPane = alert.getDialogPane();
                                dialogPane.getStylesheets().add(getClass().getResource("/shared/ui/css/DiscordTheme.css").toExternalForm());
                                dialogPane.getStyleClass().add("dialog-pane");
                                alert.showAndWait();
                            });

                            Button btnDelete = new Button("Delete");
                            btnDelete.getStyleClass().add("admin-danger-button");
                            btnDelete.setOnAction(e -> showDeleteConfirmation());

                            buttons.getChildren().addAll(btnUpdate, btnFriends, btnHistory, btnLock, btnDelete);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        });
    }

    private void applyFilters() {
        String searchText = searchBarController.searchField.getText() != null ? searchBarController.searchField.getText().toLowerCase() : "";
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        filteredData.setPredicate(user -> {
            boolean matchesSearch = user.fullname.toLowerCase().contains(searchText)
                    || user.username.toLowerCase().contains(searchText)
                    || user.status.toLowerCase().contains(searchText);

            LocalDate created = LocalDate.parse(user.createdAt, formatter);
            boolean matchesDate = true;
            if (start != null) matchesDate &= !created.isBefore(start);
            if (end != null) matchesDate &= !created.isAfter(end);

            return matchesSearch && matchesDate;
        });
    }

    private void applySort() {
        String selected = sortCombo.getValue();
        if (selected == null) return;

        Comparator<User> comparator;
        switch (selected) {
            case "Name (A-Z)" -> comparator = Comparator.comparing(u -> u.fullname.toLowerCase());
            case "Created Date (Newest)" -> comparator = (u1, u2) -> u2.createdAt.compareTo(u1.createdAt);
            default -> { return; }
        }
        FXCollections.sort(masterData, comparator);
    }

    private void showDeleteConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete this user?");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/shared/ui/css/DiscordTheme.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    @FXML private void openPasswordRequests(ActionEvent event) {
        AdminSceneSwitcher.openPopup("/admin/ui/fxml/AdminPasswordRequestsView.fxml", "Password Reset Requests");
    }

    @FXML private void openAddUser(ActionEvent event) {
        AdminSceneSwitcher.openPopup("/admin/ui/fxml/AdminAddUserView.fxml", "Add New User");
    }

    @FXML private void goToHistory(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminLoginHistoryView.fxml"); }
    @FXML private void goToGroups(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminGroupView.fxml"); }
    @FXML private void goToReports(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/admin/ui/fxml/AdminReportView.fxml"); }

    public static class User {
        String username, fullname, address, dob, email, gender, status, createdAt;
        public User(String u, String f, String a, String d, String e, String g, String s, String c) {
            this.username = u; this.fullname = f;
            this.address = a; this.dob = d;
            this.email = e; this.gender = g;
            this.status = s; this.createdAt = c;
        }
    }
}