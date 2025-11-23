package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList; // Import for sorting
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.beans.property.SimpleStringProperty;
import java.util.Comparator; // Import for sorting logic

public class AdminUserViewController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUsername, colFullname, colAddress, colDob, colEmail, colGender, colStatus, colCreated, colAction;
    @FXML private ComboBox<String> sortCombo, filterCombo;

    private ObservableList<User> masterData; // Keep reference to original data

    @FXML
    public void initialize() {
        // 1. Setup Filter/Sort
        sortCombo.setItems(FXCollections.observableArrayList("Name (A-Z)", "Created Date (Newest)"));
        filterCombo.setItems(FXCollections.observableArrayList("Name", "Username", "Status"));

        // 2. Setup Columns
        setupColumn(colUsername, data -> data.username);
        setupColumn(colFullname, data -> data.fullname);
        setupColumn(colAddress, data -> data.address);
        setupColumn(colDob, data -> data.dob);
        setupColumn(colEmail, data -> data.email);
        setupColumn(colGender, data -> data.gender);
        setupColumn(colStatus, data -> data.status);
        setupColumn(colCreated, data -> data.createdAt);
        
        // 3. Setup Action Buttons
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
                            btnUpdate.setOnAction(e -> AdminSceneSwitcher.openPopup("/fxml/AdminUpdateUserView.fxml", "Update User"));

                            Button btnFriends = new Button("Friends");
                            btnFriends.getStyleClass().add("admin-action-button");
                            btnFriends.setOnAction(e -> AdminSceneSwitcher.openPopup("/fxml/AdminUserFriendsView.fxml", "Friend List"));

                            Button btnHistory = new Button("History");
                            btnHistory.getStyleClass().add("admin-action-button");
                            btnHistory.setOnAction(e -> AdminSceneSwitcher.openPopup("/fxml/AdminUserHistoryView.fxml", "Login History"));

                            Button btnDelete = new Button("Delete");
                            btnDelete.getStyleClass().add("admin-danger-button");
                            btnDelete.setOnAction(e -> showDeleteConfirmation());

                            buttons.getChildren().addAll(btnUpdate, btnFriends, btnHistory, btnDelete);
                            setGraphic(buttons);
                        }
                    }
                };
            }
        });

        // 4. Add Dummy Data
        masterData = FXCollections.observableArrayList(
            new User("john_doe", "John Doe", "123 Street, NY", "1990-01-01", "john@example.com", "Male", "Active", "2023-01-10"),
            new User("jane_smith", "Jane Smith", "456 Avenue, CA", "1992-05-10", "jane@test.com", "Female", "Active", "2023-02-15"),
            new User("bobby_g", "Bob Gamer", "789 Road, TX", "1995-11-20", "bob@game.net", "Male", "Locked", "2023-03-01")
        );
        userTable.setItems(masterData);

        // 5. Handle Sorting Action
        sortCombo.setOnAction(event -> {
            String selected = sortCombo.getValue();
            if (selected == null) return;

            if (selected.equals("Name (A-Z)")) {
                FXCollections.sort(masterData, Comparator.comparing(u -> u.fullname.toLowerCase()));
            } else if (selected.equals("Created Date (Newest)")) {
                // Assuming YYYY-MM-DD string format, simple reverse string sort works for "Newest"
                FXCollections.sort(masterData, (u1, u2) -> u2.createdAt.compareTo(u1.createdAt));
            }
        });
    }

    private void setupColumn(TableColumn<User, String> column, Callback<User, String> valueExtractor) {
        column.setCellValueFactory(data -> new SimpleStringProperty(valueExtractor.call(data.getValue())));
        column.setReorderable(false);
        column.setSortable(false); // Disable click-to-sort
    }

    @FXML
    private void openPasswordRequests(ActionEvent event) {
        AdminSceneSwitcher.openPopup("/fxml/AdminPasswordRequestsView.fxml", "Password Reset Requests");
    }

    @FXML
    private void openAddUser(ActionEvent event) {
        AdminSceneSwitcher.openPopup("/fxml/AdminAddUserView.fxml", "Add New User");
    }

    private void showDeleteConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete this user?");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/DiscordTheme.css").toExternalForm());
        dialogPane.getStyleClass().add("dialog-pane");
        alert.showAndWait();
    }

    @FXML private void goToHistory(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminLoginHistoryView.fxml"); }
    @FXML private void goToGroups(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminGroupView.fxml"); }
    @FXML private void goToReports(ActionEvent event) { AdminSceneSwitcher.switchScene(event, "/fxml/AdminReportView.fxml"); }

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